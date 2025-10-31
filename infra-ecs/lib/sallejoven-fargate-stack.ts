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
} from 'aws-cdk-lib';

export class SalleJovenFargateStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', { vpcId: 'vpc-09f408a62930f6f1d' });
    const cluster = new ecs.Cluster(this, 'Cluster', { vpc });
    const repo = ecr.Repository.fromRepositoryName(this, 'ApiEcrRepo', 'sallejoven-api');

    // ====== PARAMS ======
    const CERT_ARN = 'arn:aws:acm:eu-north-1:659925004462:certificate/23c87695-563f-4904-b380-a453435bbd24';
    const RDS_SG_ID = 'sg-08d1b1505131491f6';
    const CONTAINER_PORT = 5000;
    const HEALTH_PATH = '/public/info';

    // Secrets
    const dbSecret   = secretsmanager.Secret.fromSecretNameV2(this, 'DbSecret',   'prod/sallejoven/db');
    const mailSecret = secretsmanager.Secret.fromSecretNameV2(this, 'MailSecret', 'prod/sallejoven/mail');

    // ====== SECURITY GROUPS ======
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
    });

    const cert = acm.Certificate.fromCertificateArn(this, 'AlbCert', CERT_ARN);
    const listenerHttps = alb.addListener('HttpsListener', {
      port: 443,
      certificates: [cert],
      protocol: elbv2.ApplicationProtocol.HTTPS,
      sslPolicy: elbv2.SslPolicy.TLS13_RES,
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

    // ====== TASK DEF + CONTAINER ======
    const logGroup = new logs.LogGroup(this, 'ApiLogGroup', {
      retention: logs.RetentionDays.ONE_MONTH,
    });

    const execRole = new iam.Role(this, 'TaskExecutionRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'),
      ],
    });

    const taskDef = new ecs.FargateTaskDefinition(this, 'TaskDef', {
      cpu: 512,
      memoryLimitMiB: 1024,
      executionRole: execRole,
    });

    // Secrets access (ambos roles)
    dbSecret.grantRead(execRole);
    dbSecret.grantRead(taskDef.taskRole);
    mailSecret.grantRead(execRole);
    mailSecret.grantRead(taskDef.taskRole);

    const container = taskDef.addContainer('ApiContainer', {
      image: ecs.ContainerImage.fromEcrRepository(repo, 'latest'),
      logging: ecs.LogDrivers.awsLogs({ logGroup, streamPrefix: 'api' }),
      environment: {
        SERVER_PORT: String(CONTAINER_PORT),
        SPRING_PROFILES_ACTIVE: 'prod',
        SPRING_JPA_HIBERNATE_DDL_AUTO: 'none',
        SPRING_DATASOURCE_URL:
          'jdbc:postgresql://database-salle.cju6gook2cqu.eu-north-1.rds.amazonaws.com:5432/postgres?sslmode=require',
        JAVA_TOOL_OPTIONS: '-XX:MaxRAMPercentage=75 -XX:+UseStringDeduplication -XX:+ExitOnOutOfMemoryError',
        AWS_REGION: 'eu-north-1',
      },
      secrets: {
        SPRING_DATASOURCE_USERNAME: ecs.Secret.fromSecretsManager(dbSecret, 'username'),
        SPRING_DATASOURCE_PASSWORD: ecs.Secret.fromSecretsManager(dbSecret, 'password'),
        MAIL_USERNAME: ecs.Secret.fromSecretsManager(mailSecret, 'username'),
        MAIL_PASSWORD: ecs.Secret.fromSecretsManager(mailSecret, 'password'),
      },
      essential: true,
    });

    container.addPortMappings({ containerPort: CONTAINER_PORT });

    // ====== SERVICE ======
    const service = new ecs.FargateService(this, 'Service', {
      cluster,
      taskDefinition: taskDef,
      desiredCount: 1,
      assignPublicIp: true,
      securityGroups: [serviceSg],
      circuitBreaker: { enable: true, rollback: true },
      vpcSubnets: { subnets: vpc.publicSubnets },
      healthCheckGracePeriod: cdk.Duration.seconds(240),
    });

    // ====== TARGET GROUP ======
    // IMPORTANTE: no asociar aquí "targets: [service]" para evitar duplicados
    const tg = new elbv2.ApplicationTargetGroup(this, 'TargetGroup', {
      vpc,
      protocol: elbv2.ApplicationProtocol.HTTP,
      port: CONTAINER_PORT,
      targetType: elbv2.TargetType.IP,
      deregistrationDelay: cdk.Duration.seconds(30),
      healthCheck: {
        path: HEALTH_PATH,
        healthyHttpCodes: '200-399',
        interval: cdk.Duration.seconds(20),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 5,
      },
    });

    // Asociación ÚNICA del Service al TG
    service.attachToApplicationTargetGroup(tg);

    // Listener -> TG
    listenerHttps.addTargetGroups('ApiTg', { targetGroups: [tg] });

    // ====== AUTO SCALING ======
    const scalable = service.autoScaleTaskCount({
      minCapacity: 1,
      maxCapacity: 2, // cap de coste
    });

    scalable.scaleOnCpuUtilization('CpuScaling', {
      targetUtilizationPercent: 60,
      scaleInCooldown: cdk.Duration.seconds(120),
      scaleOutCooldown: cdk.Duration.seconds(60),
    });

    scalable.scaleOnRequestCount('ReqScaling', {
      targetGroup: tg,
      requestsPerTarget: 100,
    });

    // ====== S3 PERMISSIONS (para presigned y operaciones directas) ======
    const bucketArn = 'arn:aws:s3:::sallejoven-events';
    const bucketObjectsArn = `${bucketArn}/*`;

    // Usa addToTaskRolePolicy (no addToPolicy) para evitar el error de IRole
    taskDef.addToTaskRolePolicy(new iam.PolicyStatement({
      actions: [
        's3:GetObject',             // GET / HEAD
        's3:PutObject',             // PUT
        's3:DeleteObject',          // DELETE
        's3:AbortMultipartUpload',  // multipart
      ],
      resources: [bucketObjectsArn],
    }));

    taskDef.addToTaskRolePolicy(new iam.PolicyStatement({
      actions: ['s3:ListBucket'],
      resources: [bucketArn],
    }));

    // ====== OUTPUTS ======
    new cdk.CfnOutput(this, 'AlbDns', { value: alb.loadBalancerDnsName });
    new cdk.CfnOutput(this, 'ServiceSgId', { value: serviceSg.securityGroupId });
    new cdk.CfnOutput(this, 'ClusterName', { value: cluster.clusterName });
    new cdk.CfnOutput(this, 'VpcId', { value: vpc.vpcId });
    new cdk.CfnOutput(this, 'EcrRepositoryUri', { value: repo.repositoryUri });
  }
}
