import * as cdk from 'aws-cdk-lib';
import { SalleJovenFargateStack } from '../lib/sallejoven-fargate-stack';

const app = new cdk.App();

function ctx(key: string, def?: string) {
  return (app.node.tryGetContext(key) as string | undefined) ?? process.env[key.toUpperCase()] ?? def;
}

const vpcId       = ctx('vpcId');
const certArn     = ctx('certArn');
const rdsSgId     = ctx('rdsSgId');
const ecrRepoName = ctx('ecrRepoName');

const containerPort     = Number(ctx('containerPort', '5000'));
const healthPath        = ctx('healthPath', '/public/info')!;
const usePublicSubnets  = (ctx('usePublicSubnets', 'true') === 'true');

// Falla pronto si faltan los imprescindibles
for (const [k, v] of Object.entries({ vpcId, certArn, rdsSgId, ecrRepoName })) {
  if (!v) throw new Error(`Missing required context/env: ${k}`);
}

new SalleJovenFargateStack(app, 'SalleJovenFargate', {
  env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: 'eu-north-1' },

  // Props “fuertes” que espera tu stack mejorado
  vpcId: vpcId!,
  certArn: certArn!,
  rdsSecurityGroupId: rdsSgId!,
  ecrRepoName: ecrRepoName!,
  containerPort,
  healthPath,
  usePublicSubnets,
});
