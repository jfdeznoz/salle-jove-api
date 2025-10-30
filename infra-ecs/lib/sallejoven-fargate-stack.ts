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

    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', {
      vpcId: 'vpc-09f408a62930f6f1d'
    });

    const cluster = new ecs.Cluster(this, 'Cluster', { vpc });

    const repo = new ecr.Repository(this, 'ApiEcrRepo', {
      repositoryName: 'sallejoven-api',
      imageScanOnPush: true,
      lifecycleRules: [
        { maxImageCount: 10 } // conserva los últimos 10 tags
      ]
    });

    /* =========================
     *  0) PARAMS A RELLENAR
     * ========================= */
    const CERT_ARN = 'arn:aws:acm:eu-north-1:659925004462:certificate/23c87695-563f-4904-b380-a453435bbd24';
    const RDS_SG_ID = 'sg-08d1b1505131491f6';
    const CONTAINER_PORT = 5000;
    const HEALTH_PATH = '/actuator/health';

    const dbSecret = secretsmanager.Secret.fromSecretNameV2(
      this,
      'DbSecret',
      'prod/sallejoven/db' // o usa fromSecretCompleteArn si prefieres el ARN
    );

    /* =========================
     *  1) SECURITY GROUPS
     * ========================= */
    // SG del ALB (entradas 80/443 desde internet)
    const albSg = new ec2.SecurityGroup(this, 'AlbSg', {
      vpc,
      allowAllOutbound: true,
      description: 'ALB SG (80/443 from internet)',
    });
    albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80), 'HTTP from anywhere');
    albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), 'HTTPS from anywhere');

    // SG del servicio ECS (solo tráfico desde el ALB al puerto de la app)
    const serviceSg = new ec2.SecurityGroup(this, 'ServiceSg', {
      vpc,
      allowAllOutbound: true,
      description: 'Fargate Service SG (ingress only from ALB)',
    });
    serviceSg.addIngressRule(albSg, ec2.Port.tcp(CONTAINER_PORT), 'App port from ALB');

    // autoriza ECS->RDS: 5432
    const rdsSg = ec2.SecurityGroup.fromSecurityGroupId(this, 'RdsSg', RDS_SG_ID, { mutable: false });
    new ec2.CfnSecurityGroupIngress(this, 'RdsFromService5432', {
      groupId: rdsSg.securityGroupId,
      sourceSecurityGroupId: serviceSg.securityGroupId,
      ipProtocol: 'tcp',
      fromPort: 5432,
      toPort: 5432,
      description: 'Allow ECS service to access Postgres on 5432',
    });

    /* =========================
     *  2) LOAD BALANCER + LISTENERS
     * ========================= */
    const alb = new elbv2.ApplicationLoadBalancer(this, 'Alb', {
      vpc,
      internetFacing: true,
      securityGroup: albSg,
      vpcSubnets: { subnets: vpc.publicSubnets }, // tus 3 públicas
    });

    let listenerHttps: elbv2.ApplicationListener;

    if (CERT_ARN && CERT_ARN.startsWith('arn:aws:acm:')) {
      const cert = acm.Certificate.fromCertificateArn(this, 'AlbCert', CERT_ARN);

      // HTTPS listener (443)
      listenerHttps = alb.addListener('HttpsListener', {
        port: 443,
        certificates: [cert],
        protocol: elbv2.ApplicationProtocol.HTTPS,
        open: true,
      });

      // HTTP listener (80) que redirige a HTTPS
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
      // Sin cert → solo HTTP (80)
      listenerHttps = alb.addListener('HttpListener', {
        port: 80,
        protocol: elbv2.ApplicationProtocol.HTTP,
        open: true,
      });
    }

    /* =========================
     *  3) TASK DEF + CONTAINER
     * ========================= */
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
      cpu: 256,
      memoryLimitMiB: 512,
      executionRole: execRole, // 👈 ahora es explícita
    });

    dbSecret.grantRead(execRole);
    dbSecret.grantRead(taskDef.taskRole);

    const repository = repo;

    const container = taskDef.addContainer('ApiContainer', {
      image: ecs.ContainerImage.fromEcrRepository(repository, 'latest'),
      logging: ecs.LogDrivers.awsLogs({ logGroup, streamPrefix: 'api' }),
      environment: {
        SERVER_PORT: String(CONTAINER_PORT),
        SPRING_PROFILES_ACTIVE: 'prod',
        SPRING_JPA_HIBERNATE_DDL_AUTO: 'none',
        SPRING_DATASOURCE_URL: 'jdbc:postgresql://database-salle.cju6gook2cqu.eu-north-1.rds.amazonaws.com:5432/postgres?sslmode=require',
      },
      secrets: {
        SPRING_DATASOURCE_USERNAME: ecs.Secret.fromSecretsManager(dbSecret, 'username'),
        SPRING_DATASOURCE_PASSWORD: ecs.Secret.fromSecretsManager(dbSecret, 'password'),
      },
      essential: true,
    });

    container.addPortMappings({ containerPort: CONTAINER_PORT });

    /* =========================
     *  4) FARGATE SERVICE
     * ========================= */
    const service = new ecs.FargateService(this, 'Service', {
      cluster,
      taskDefinition: taskDef,
      desiredCount: 1,
      assignPublicIp: true,          // estás en subnets públicas sin NAT
      securityGroups: [serviceSg],
      circuitBreaker: { enable: true, rollback: true },
      vpcSubnets: { subnets: vpc.publicSubnets },
    });

    /* =========================
     *  5) TARGET GROUP + HEALTHCHECK
     * ========================= */
    const tg = new elbv2.ApplicationTargetGroup(this, 'TargetGroup', {
      vpc,
      protocol: elbv2.ApplicationProtocol.HTTP,
      port: CONTAINER_PORT,
      targetType: elbv2.TargetType.IP,
      targets: [service],
      healthCheck: {
        path: HEALTH_PATH,
        healthyHttpCodes: '200-399',
        interval: cdk.Duration.seconds(20),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 5,
      },
    });
    listenerHttps.addTargetGroups('ApiTg', { targetGroups: [tg] });

    new cdk.CfnOutput(this, 'AlbDns', { value: alb.loadBalancerDnsName });
    new cdk.CfnOutput(this, 'ServiceSgId', { value: serviceSg.securityGroupId });
    new cdk.CfnOutput(this, 'ClusterName', { value: cluster.clusterName });
    new cdk.CfnOutput(this, 'VpcId', { value: vpc.vpcId });
    new cdk.CfnOutput(this, 'EcrRepositoryUri', { value: repo.repositoryUri });
  }
}
