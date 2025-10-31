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

export interface SalleJovenFargateStackProps extends cdk.StackProps {
  vpcId: string;                                    // vpc-09f408a62930f6f1d
  certArn: string;                                  // arn:aws:acm:eu-north-1:...:certificate/...
  rdsSecurityGroupId: string;                       // sg-08d1b1505131491f6
  ecrRepoName: string;                              // sallejoven-api
  containerPort?: number;                           // 5000 defecto
  healthPath?: string;                              // /public/info defecto
  usePublicSubnets?: boolean;                       // true defecto
}

export class SalleJovenFargateStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: SalleJovenFargateStackProps) {
    super(scope, id, props);

    // ====== PARAMS / DEFAULTS ======
    const CONTAINER_PORT = props.containerPort ?? 5000;
    const HEALTH_PATH    = props.healthPath ?? '/public/info';
    const USE_PUBLIC     = props.usePublicSubnets ?? true;

    // ====== VPC ======
    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', { vpcId: props.vpcId });

    // ====== CLUSTER ======
    const cluster = new ecs.Cluster(this, 'Cluster', {
      vpc,
      enableFargateCapacityProviders: true,
      containerInsights: true,
    });

    // ====== ECR ======
    const repo = ecr.Repository.fromRepositoryName(this, 'ApiEcrRepo', props.ecrRepoName);

    // ====== SECRETS (DB) ======
    const dbSecret = secretsmanager.Secret.fromSecretNameV2(this, 'DbSecret', 'prod/sallejoven/db');

    // ====== SGs ======
    const albSg = new ec2.SecurityGroup(this, 'AlbSg', {
      vpc,
      allowAllOutbound: true,
      description: 'ALB SG (80/443 from internet)',
    });
    albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80),  'Allow HTTP');
    albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), 'Allow HTTPS');

    const serviceSg = new ec2.SecurityGroup(this, 'ServiceSg', {
      vpc,
      allowAllOutbound: true, // necesario para salir a RDS/S3/etc
      description: 'Fargate Service SG (ingress only from ALB)',
    });
    serviceSg.addIngressRule(albSg, ec2.Port.tcp(CONTAINER_PORT), 'ALB -> Service container port');

    const rdsSg = ec2.SecurityGroup.fromSecurityGroupId(this, 'RdsSg', props.rdsSecurityGroupId, { mutable: false });

    // Regla de acceso a RDS (no podemos usar addIngressRule sobre un SG inmutable)
    new ec2.CfnSecurityGroupIngress(this, 'RdsIngressFromServiceOn5432', {
      groupId: rdsSg.securityGroupId,
      sourceSecurityGroupId: serviceSg.securityGroupId,
      ipProtocol: 'tcp',
      fromPort: 5432,
      toPort: 5432,
      description: 'Allow ECS service to access Postgres on 5432',
    });

    // ====== ALB ======
    const alb = new elbv2.ApplicationLoadBalancer(this, 'Alb', {
      vpc,
      internetFacing: true,
      securityGroup: albSg,
      vpcSubnets: USE_PUBLIC ? { subnets: vpc.publicSubnets } : undefined,
      http2Enabled: true,
    });

    const cert = acm.Certificate.fromCertificateArn(this, 'AlbCert', props.certArn);

    // HTTPS Listener
    const httpsListener = alb.addListener('HttpsListener', {
      port: 443,
      protocol: elbv2.ApplicationProtocol.HTTPS,
      certificates: [cert],
      open: true,
    });

    // HTTP → HTTPS redirect
    const httpListener = alb.addListener('HttpListener', {
      port: 80,
      protocol: elbv2.ApplicationProtocol.HTTP,
      open: true,
    });
    httpListener.addAction('RedirectToHttps', elbv2.ListenerAction.redirect({
      protocol: 'HTTPS',
      port: '443',
      permanent: true,
    }));

    // ====== LOGS ======
    const logGroup = new logs.LogGroup(this, 'ApiLogGroup', {
      retention: logs.RetentionDays.ONE_MONTH,
      removalPolicy: cdk.RemovalPolicy.RETAIN, // en prod retenemos
      logGroupName: `/ecs/sallejoven/api`,     // nombre estable y reconocible
    });

    // ====== ROLES ======
    const executionRole = new iam.Role(this, 'TaskExecutionRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'),
      ],
    });

    const taskRole = new iam.Role(this, 'TaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
      description: 'App task role (business permissions go here)',
    });

    // Permiso de lectura del secreto tanto a executionRole como a taskRole
    dbSecret.grantRead(executionRole);
    dbSecret.grantRead(taskRole);

    // ====== TASK DEF ======
    const taskDef = new ecs.FargateTaskDefinition(this, 'TaskDef', {
      cpu: 256,
      memoryLimitMiB: 512,
      executionRole,
      taskRole,
      runtimePlatform: { // explícito por claridad
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.X86_64,
      },
    });

    const container = taskDef.addContainer('ApiContainer', {
      image: ecs.ContainerImage.fromEcrRepository(repo, 'latest'),
      essential: true,
      logging: ecs.LogDrivers.awsLogs({ logGroup, streamPrefix: 'api' }),
      environment: {
        SERVER_PORT: String(CONTAINER_PORT),
        SPRING_PROFILES_ACTIVE: 'prod',
        SPRING_JPA_HIBERNATE_DDL_AUTO: 'none',
        SPRING_DATASOURCE_URL:
          'jdbc:postgresql://database-salle.cju6gook2cqu.eu-north-1.rds.amazonaws.com:5432/postgres?sslmode=require',
      },
      secrets: {
        SPRING_DATASOURCE_USERNAME: ecs.Secret.fromSecretsManager(dbSecret, 'username'),
        SPRING_DATASOURCE_PASSWORD: ecs.Secret.fromSecretsManager(dbSecret, 'password'),
      },
      healthCheck: {
        // Health check de Docker (complementario al del ALB). Opcional, pero útil.
        command: ['CMD-SHELL', `curl -sf http://127.0.0.1:${CONTAINER_PORT}${HEALTH_PATH} || exit 1`],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(90),
      },
    });

    container.addPortMappings({ containerPort: CONTAINER_PORT, protocol: ecs.Protocol.TCP });

    // ====== SERVICE ======
    const service = new ecs.FargateService(this, 'Service', {
      cluster,
      taskDefinition: taskDef,
      desiredCount: 1,
      assignPublicIp: USE_PUBLIC, // si usas subredes públicas
      securityGroups: [serviceSg],
      circuitBreaker: { enable: true, rollback: true },
      vpcSubnets: USE_PUBLIC ? { subnets: vpc.publicSubnets } : undefined,
      healthCheckGracePeriod: cdk.Duration.seconds(240),
      enableECSManagedTags: true,
      propagateTags: ecs.PropagatedTagSource.SERVICE,
      capacityProviderStrategies: [
        { capacityProvider: 'FARGATE', weight: 1 }, // explícito
      ],
    });

    // ====== TARGET GROUP explícito (IP) ======
    const tg = new elbv2.ApplicationTargetGroup(this, 'ApiTg', {
      vpc,
      targetType: elbv2.TargetType.IP,            // Fargate -> IP
      protocol: elbv2.ApplicationProtocol.HTTP,   // tráﬁco interno ALB->task en claro (TLS termina en ALB)
      port: CONTAINER_PORT,
      healthCheck: {
        path: HEALTH_PATH,
        healthyHttpCodes: '200-399',
        interval: cdk.Duration.seconds(20),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 5,
      },
      deregistrationDelay: cdk.Duration.seconds(15),
      slowStart: cdk.Duration.seconds(30),        // ayuda con warm-up de Spring
      // stickinessCookieDuration: cdk.Duration.seconds(0), // desactivado por defecto
    });

    // Registrar el servicio como target del TG
    tg.addTarget(service);

    // Listener → TG
    httpsListener.addTargetGroups('HttpsToApi', {
      targetGroups: [tg],
      // Opcionalmente: condiciones/rutas si más servicios
      // conditions: [elbv2.ListenerCondition.pathPatterns(['/api/*'])],
      priority: 1,
    });

    // ====== AUTOSCALING ======
    const scalable = service.autoScaleTaskCount({ minCapacity: 1, maxCapacity: 2 });
    scalable.scaleOnCpuUtilization('CpuScaling', {
      targetUtilizationPercent: 60,
      scaleInCooldown: cdk.Duration.seconds(120),
      scaleOutCooldown: cdk.Duration.seconds(60),
    });
    scalable.scaleOnRequestCount('ReqScaling', {
      targetGroup: tg,
      requestsPerTarget: 100,
    });

    // ====== OUTPUTS ======
    new cdk.CfnOutput(this, 'AlbDns', { value: alb.loadBalancerDnsName });
    new cdk.CfnOutput(this, 'AlbArn', { value: alb.loadBalancerArn });
    new cdk.CfnOutput(this, 'TargetGroupArn', { value: tg.targetGroupArn });
    new cdk.CfnOutput(this, 'ServiceSgId', { value: serviceSg.securityGroupId });
    new cdk.CfnOutput(this, 'ClusterName', { value: cluster.clusterName });
    new cdk.CfnOutput(this, 'VpcId', { value: vpc.vpcId });
    new cdk.CfnOutput(this, 'EcrRepositoryUri', { value: repo.repositoryUri });
  }
}
