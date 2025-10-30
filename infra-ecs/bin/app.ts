import * as cdk from 'aws-cdk-lib';
import { SalleJovenFargateStack } from '../lib/sallejoven-fargate-stack';

const app = new cdk.App();

new SalleJovenFargateStack(app, 'SalleJovenFargate', {
  env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: 'eu-north-1' }
});
