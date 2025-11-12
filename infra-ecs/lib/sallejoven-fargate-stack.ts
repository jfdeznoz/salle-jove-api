import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import {
  aws_ec2 as ec2,
  aws_ecs as ecs,
  aws_ecr as ecr,
  aws_elasticloadbalancingv2 as elbv2,
  aws_certificatemanager as acm,
  aws_logs as logs,
  aws_secretsmanager as secretsmanager,
  aws_iam as iam,
  aws_ssm as ssm,
} from 'aws-cdk-lib';
import { Schedule } from 'aws-cdk-lib/aws-applicationautoscaling';

export class SalleJovenFargateStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // ====== VPC EXISTENTE ======
    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', {
      vpcId: 'vpc-09f408a62930f6f1d',
    });

    // ====== CLUSTER ECS ======
    const cluster = new ecs.Cluster(this, 'Cluster', { vpc });
    // Habilitar capacity providers (On-Demand + Spot)
    cluster.enableFargateCapacityProviders();

    // ====== ECR EXISTENTE ======
    const repo = ecr.Repository.fromRepositoryName(this, 'ApiEcrRepo', 'sallejoven-api');

    // ====== PARAMS ======
    const CERT_ARN = 'arn:aws:acm:eu-north-1:659925004462:certificate/23c87695-563f-4904-b380-a453435bbd24';
    const RDS_SG_ID = 'sg-08d1b1505131491f6';
    const CONTAINER_PORT = 5000;
    const HEALTH_PATH = '/public/info';

    // ====== SECRET DB ======
    const dbSecret = secretsmanager.Secret.fromSecretNameV2(this, 'DbSecret', 'prod/sallejoven/db');
    const mailSecret = secretsmanager.Secret.fromSecretNameV2(this, 'MailSecret', 'prod/sallejoven/mail');

    // ====== SGs ======
    const albSg = new ec2.SecurityGroup(this, 'AlbSg', {
      vpc,
      allowAllOutbound: true,
      description: 'ALB SG (80/443 from internet)',
    });
    albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80));
    albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443));

    const serviceSg = new ec2.SecurityGroup(this, 'ServiceSg', {
      vpc,
      allowAllOutbound: true,
      description: 'Fargate Service SG (ingress only from ALB)',
    });
    serviceSg.addIngressRule(albSg, ec2.Port.tcp(CONTAINER_PORT));

    const rdsSg = ec2.SecurityGroup.fromSecurityGroupId(this, 'RdsSg', RDS_SG_ID, { mutable: false });
    new ec2.CfnSecurityGroupIngress(this, 'RdsFromService5432', {
      groupId: rdsSg.securityGroupId,
      sourceSecurityGroupId: serviceSg.securityGroupId,
      ipProtocol: 'tcp',
      fromPort: 5432,
      toPort: 5432,
      description: 'Allow ECS service to access Postgres on 5432',
    });

    // ====== ALB + LISTENERS ======
    const alb = new elbv2.ApplicationLoadBalancer(this, 'Alb', {
      vpc,
      internetFacing: true,
      securityGroup: albSg,
      vpcSubnets: { subnets: vpc.publicSubnets },
      idleTimeout: cdk.Duration.seconds(30), // ↓ reduce LCUs
    });

    let listenerHttps: elbv2.ApplicationListener;
    if (CERT_ARN && CERT_ARN.startsWith('arn:aws:acm:')) {
      const cert = acm.Certificate.fromCertificateArn(this, 'AlbCert', CERT_ARN);

      listenerHttps = alb.addListener('HttpsListener', {
        port: 443,
        certificates: [cert],
        protocol: elbv2.ApplicationProtocol.HTTPS,
        open: true,
      });

      const httpListener = alb.addListener('HttpRedirect', {
        port: 80,
        protocol: elbv2.ApplicationProtocol.HTTP,
        open: true,
      });
      httpListener.addAction('RedirectToHttps', {
        action: elbv2.ListenerAction.redirect({
          protocol: 'HTTPS',
          port: '443',
          permanent: true,
        }),
      });
    } else {
      listenerHttps = alb.addListener('HttpListener', {
        port: 80,
        protocol: elbv2.ApplicationProtocol.HTTP,
        open: true,
      });
    }

    // ====== TASK DEF + CONTAINER ======
    const logGroup = new logs.LogGroup(this, 'ApiLogGroup', {
      retention: logs.RetentionDays.ONE_WEEK,
    });

    const execRole = new iam.Role(this, 'TaskExecutionRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'),
      ],
    });

    const taskDef = new ecs.FargateTaskDefinition(this, 'TaskDef', {
      cpu: 256,               // 0.25 vCPU
      memoryLimitMiB: 1024,   // 1 GB
      executionRole: execRole,
    });

    dbSecret.grantRead(execRole);
    dbSecret.grantRead(taskDef.taskRole);
    mailSecret.grantRead(execRole);
    mailSecret.grantRead(taskDef.taskRole);

    taskDef.addToTaskRolePolicy(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: ['s3:PutObject'],
      resources: [
        'arn:aws:s3:::sallejoven-events/*/inputs/jobs/*',
        'arn:aws:s3:::sallejoven-events/test/*/inputs/jobs/*',
      ],
    }));

    taskDef.addToTaskRolePolicy(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: ['sqs:SendMessage'],
      resources: ['arn:aws:sqs:eu-north-1:659925004462:report-jobs'],
    }));

    const container = taskDef.addContainer('ApiContainer', {
      image: ecs.ContainerImage.fromEcrRepository(repo, 'latest'),
      logging: ecs.LogDrivers.awsLogs({ logGroup, streamPrefix: 'api' }),
      environment: {
        PORT: String(CONTAINER_PORT),
        SPRING_PROFILES_ACTIVE: 'prod',
        SPRING_JPA_HIBERNATE_DDL_AUTO: 'none',
        SPRING_DATASOURCE_URL:
          'jdbc:postgresql://database-salle.cju6gook2cqu.eu-north-1.rds.amazonaws.com:5432/postgres?sslmode=require',
      },
      secrets: {
        SPRING_DATASOURCE_USERNAME: ecs.Secret.fromSecretsManager(dbSecret, 'username'),
        SPRING_DATASOURCE_PASSWORD: ecs.Secret.fromSecretsManager(dbSecret, 'password'),
        SPRING_MAIL_USERNAME: ecs.Secret.fromSecretsManager(mailSecret, 'username'),
        SPRING_MAIL_PASSWORD: ecs.Secret.fromSecretsManager(mailSecret, 'password'),
      },
      essential: true,
    });

    container.addPortMappings({ containerPort: CONTAINER_PORT });

    // ====== SERVICES (Día y Noche) ======

    // --- Servicio de día: on-demand base + spot para picos (07:00–00:05)
    const serviceDay = new ecs.FargateService(this, 'ServiceDay', {
      cluster,
      taskDefinition: taskDef,
      desiredCount: 1,
      assignPublicIp: true,
      securityGroups: [serviceSg],
      circuitBreaker: { enable: true, rollback: true },
      vpcSubnets: { subnets: vpc.publicSubnets },
      healthCheckGracePeriod: cdk.Duration.seconds(240),
      capacityProviderStrategies: [
        { capacityProvider: 'FARGATE', base: 1, weight: 1 }, // on-demand garantizado de día
        { capacityProvider: 'FARGATE_SPOT', weight: 2 },     // spot para picos
      ],
    });

    const scalingDay = serviceDay.autoScaleTaskCount({
      minCapacity: 1,
      maxCapacity: 3,
    });
    scalingDay.scaleOnCpuUtilization('Cpu60Day', {
      targetUtilizationPercent: 60,
      scaleInCooldown: cdk.Duration.seconds(180),
      scaleOutCooldown: cdk.Duration.seconds(45),
    });
    scalingDay.scaleOnMemoryUtilization('Mem70Day', {
      targetUtilizationPercent: 70,
      scaleInCooldown: cdk.Duration.seconds(180),
      scaleOutCooldown: cdk.Duration.seconds(45),
    });
    // Horarios (Europe/Madrid) con solape para evitar 503
    // Enciende 06:55
    scalingDay.scaleOnSchedule('DayOn_0655', {
      schedule: Schedule.cron({ minute: '55', hour: '6' }),
      minCapacity: 1, maxCapacity: 3,
    });
    // Apaga 00:05
    scalingDay.scaleOnSchedule('DayOff_0005', {
      schedule: Schedule.cron({ minute: '5', hour: '0' }),
      minCapacity: 0, maxCapacity: 0,
    });

    // --- Servicio de noche: solo Spot (23:55–07:05)
    const serviceNight = new ecs.FargateService(this, 'ServiceNight', {
      cluster,
      taskDefinition: taskDef,
      desiredCount: 0, // arrancará por horario
      assignPublicIp: true,
      securityGroups: [serviceSg],
      circuitBreaker: { enable: true, rollback: true },
      vpcSubnets: { subnets: vpc.publicSubnets },
      healthCheckGracePeriod: cdk.Duration.seconds(240),
      capacityProviderStrategies: [
        { capacityProvider: 'FARGATE_SPOT', weight: 1 }, // solo Spot por la noche
      ],
    });

    const scalingNight = serviceNight.autoScaleTaskCount({
      minCapacity: 0,
      maxCapacity: 2,
    });
    // Enciende 23:55
    scalingNight.scaleOnSchedule('NightOn_2355', {
      schedule: Schedule.cron({ minute: '55', hour: '23' }),
      minCapacity: 1, maxCapacity: 2,
    });
    // Apaga 07:05
    scalingNight.scaleOnSchedule('NightOff_0705', {
      schedule: Schedule.cron({ minute: '5', hour: '7' }),
      minCapacity: 0, maxCapacity: 0,
    });

    // ====== LISTENER TARGETS ======
    const tg = listenerHttps.addTargets('ApiTargets5000', {
      protocol: elbv2.ApplicationProtocol.HTTP,
      port: CONTAINER_PORT,
      targets: [serviceDay], // registra el servicio de día primero
      healthCheck: {
        path: HEALTH_PATH,
        healthyHttpCodes: '200-399',
        interval: cdk.Duration.seconds(20),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 5,
      },
    });
    tg.setAttribute('deregistration_delay.timeout_seconds', '15');

    // ➕ añade también el servicio nocturno al mismo TG
    tg.addTarget(serviceNight);

    // ====== PARAMS SSM (del servicio de día como referencia principal) ======
    new ssm.StringParameter(this, 'ParamEcsClusterArn', {
      parameterName: '/sallejoven/ecs/cluster-arn',
      stringValue: cluster.clusterArn,
    });
    new ssm.StringParameter(this, 'ParamEcsClusterName', {
      parameterName: '/sallejoven/ecs/cluster-name',
      stringValue: cluster.clusterName,
    });
    new ssm.StringParameter(this, 'ParamEcsServiceDayArn', {
      parameterName: '/sallejoven/ecs/service-day-arn',
      stringValue: serviceDay.serviceArn,
    });
    new ssm.StringParameter(this, 'ParamEcsServiceDayName', {
      parameterName: '/sallejoven/ecs/service-day-name',
      stringValue: serviceDay.serviceName,
    });
    new ssm.StringParameter(this, 'ParamEcsServiceNightArn', {
      parameterName: '/sallejoven/ecs/service-night-arn',
      stringValue: serviceNight.serviceArn,
    });
    new ssm.StringParameter(this, 'ParamEcsServiceNightName', {
      parameterName: '/sallejoven/ecs/service-night-name',
      stringValue: serviceNight.serviceName,
    });

    // ====== OUTPUTS ======
    new cdk.CfnOutput(this, 'AlbDns', { value: alb.loadBalancerDnsName });
    new cdk.CfnOutput(this, 'ServiceDayName', { value: serviceDay.serviceName });
    new cdk.CfnOutput(this, 'ServiceNightName', { value: serviceNight.serviceName });
    new cdk.CfnOutput(this, 'ServiceSgId', { value: serviceSg.securityGroupId });
    new cdk.CfnOutput(this, 'ClusterName', { value: cluster.clusterName });
    new cdk.CfnOutput(this, 'VpcId', { value: vpc.vpcId });
    new cdk.CfnOutput(this, 'EcrRepositoryUri', { value: repo.repositoryUri });
  }
}
