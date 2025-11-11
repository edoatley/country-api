# CloudFormation GitHub Action Evaluation

## Action: `aws-actions/aws-cloudformation-github-deploy`

## Current Approach Analysis

### What `deploy-stack.sh` Does:
1. **Pre-deployment Checks:**
   - Gets Lambda execution role ARN from CloudFormation stack
   - Checks DynamoDB table existence
   - Verifies S3 object exists
   - Handles stack status (ROLLBACK_COMPLETE/ROLLBACK_FAILED) - deletes and recreates

2. **Deployment:**
   - Runs `aws cloudformation deploy` with parameter overrides
   - Handles capabilities (CAPABILITY_NAMED_IAM)
   - Error handling with stack events

3. **Post-deployment:**
   - Retrieves stack outputs (Lambda ARN, API Gateway URL)

### Current Workflow Usage:
```yaml
- name: Deploy stack using CloudFormation
  run: |
    cd infrastructure
    chmod +x deploy-stack.sh
    export API_KEY="${{ secrets.API_KEY }}"
    ./deploy-stack.sh staging ${{ env.AWS_REGION }} "" ${{ env.S3_BUCKET }} ${{ steps.upload_s3.outputs.s3-key }}
```

## GitHub Action Capabilities

### Supported Inputs (from documentation):
- `name`: Stack name ✅
- `template`: Template file path ✅
- `parameter-overrides`: Comma-separated parameter overrides ✅
- `capabilities`: IAM capabilities ✅
- `no-fail-on-empty-changeset`: Don't fail if no changes ✅
- `region`: AWS region ✅
- `role-arn`: IAM role to assume (optional)
- `tags`: Stack tags (optional)

### What the Action Does:
1. Creates/updates CloudFormation stack
2. Handles change sets automatically
3. Provides standardized error messages
4. Can output stack outputs

### What the Action Does NOT Do:
1. ❌ Pre-deployment checks (role ARN retrieval, DynamoDB check, S3 verification)
2. ❌ Stack status handling (ROLLBACK_COMPLETE/ROLLBACK_FAILED)
3. ❌ Custom error reporting with stack events
4. ❌ Pre-deployment validation

## Comparison

| Feature | Current (deploy-stack.sh) | GitHub Action | Notes |
|---------|---------------------------|---------------|-------|
| Parameter overrides | ✅ | ✅ | Both support |
| Capabilities | ✅ | ✅ | Both support |
| Pre-deployment checks | ✅ | ❌ | Action doesn't do this |
| Stack status handling | ✅ | ❌ | Action doesn't handle ROLLBACK states |
| Error reporting | ✅ Custom | ✅ Standardized | Action has better GitHub integration |
| Stack outputs | ✅ Manual | ✅ Via outputs | Action can expose outputs |
| Change set handling | Manual | ✅ Automatic | Action advantage |
| Template validation | ❌ | ✅ | Action validates templates |
| Simpler YAML | ❌ | ✅ | Action advantage |

## Recommendation

### Option 1: Hybrid Approach (Recommended)
**Keep `deploy-stack.sh` for pre-deployment checks, use GitHub Action for deployment**

**Pros:**
- Maintains all existing functionality
- Simplifies workflow YAML (no need to cd into infrastructure)
- Better error reporting from GitHub Action
- Automatic change set handling

**Cons:**
- Still need to maintain `deploy-stack.sh` for pre-checks
- Slightly more complex (two-step process)

**Implementation:**
1. Keep pre-deployment checks in workflow (using our helper scripts)
2. Replace `deploy-stack.sh` deployment step with GitHub Action
3. Use action outputs for stack outputs

### Option 2: Full Migration to GitHub Action
**Replace `deploy-stack.sh` entirely with GitHub Action + workflow steps**

**Pros:**
- Simpler overall (no bash script for deployment)
- Better GitHub Actions integration
- Automatic change set handling

**Cons:**
- Need to move pre-deployment checks to workflow steps
- Need to handle stack status manually in workflow
- More workflow YAML complexity

### Option 3: Keep Current Approach
**Continue using `deploy-stack.sh` as-is**

**Pros:**
- All logic in one place
- Works well currently
- Full control over deployment process

**Cons:**
- More complex workflow YAML
- Manual change set handling
- Less GitHub Actions integration

## Decision: Hybrid Approach

**Rationale:**
1. We've already created helper scripts for pre-deployment checks (get-lambda-role-arn.sh, etc.)
2. The GitHub Action excels at the actual deployment (change sets, error handling)
3. We can simplify the workflow while maintaining all functionality
4. Best of both worlds: custom logic where needed, standardized deployment

## Implementation Plan

1. **Keep pre-deployment steps** (using helper scripts):
   - Get Lambda execution role ARN
   - Deploy execution roles if needed
   - Verify S3 object exists (already done in upload step)

2. **Replace deployment step** with GitHub Action:
   - Use `aws-actions/aws-cloudformation-github-deploy`
   - Pass parameters via `parameter-overrides`
   - Use action outputs for stack outputs

3. **Handle stack status** in workflow (if needed):
   - Check for ROLLBACK_COMPLETE/ROLLBACK_FAILED before deployment
   - Delete stack if in bad state

4. **Update error handling**:
   - Use action's built-in error reporting
   - Add custom steps for detailed stack events if needed

## Example Implementation

```yaml
- name: Check stack status (handle ROLLBACK states)
  id: stack_status
  run: |
    STACK_NAME="country-service-staging"
    STATUS=$(aws cloudformation describe-stacks \
      --stack-name "$STACK_NAME" \
      --region ${{ env.AWS_REGION }} \
      --query 'Stacks[0].StackStatus' \
      --output text 2>/dev/null || echo "NOT_FOUND")
    
    if [ "$STATUS" = "ROLLBACK_COMPLETE" ] || [ "$STATUS" = "ROLLBACK_FAILED" ]; then
      echo "Stack in $STATUS state, deleting..."
      aws cloudformation delete-stack --stack-name "$STACK_NAME" --region ${{ env.AWS_REGION }}
      aws cloudformation wait stack-delete-complete --stack-name "$STACK_NAME" --region ${{ env.AWS_REGION }}
      echo "stack_action=delete" >> $GITHUB_OUTPUT
    else
      echo "stack_action=deploy" >> $GITHUB_OUTPUT
    fi

- name: Deploy CloudFormation stack
  id: deploy
  uses: aws-actions/aws-cloudformation-github-deploy@v1
  with:
    name: country-service-staging
    template: infrastructure/lambda-api-gateway.yaml
    parameter-overrides: |
      Environment=staging,
      LambdaExecutionRoleArn=${{ steps.lambda_role.outputs.role_arn }},
      CodeS3Bucket=${{ env.S3_BUCKET }},
      CodeS3Key=${{ steps.upload_s3.outputs.s3-key }},
      ApiKey=${{ secrets.API_KEY }},
      DynamoDBTableName=Countries,
      Timeout=30,
      MemorySize=512
    capabilities: CAPABILITY_NAMED_IAM
    region: ${{ env.AWS_REGION }}
    no-fail-on-empty-changeset: "1"
```

## Next Steps

1. Test the GitHub Action in a test workflow
2. Compare error messages and output
3. Verify stack outputs are accessible
4. Implement hybrid approach if successful
5. Update documentation

