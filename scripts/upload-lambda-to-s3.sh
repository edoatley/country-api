#!/bin/bash

# Upload Lambda package to S3 with versioned key
# Usage: ./upload-lambda-to-s3.sh <jar-file> <s3-bucket> <region> [version]
#   jar-file: Path to Lambda JAR file
#   s3-bucket: S3 bucket name
#   region: AWS region
#   version: Optional version (if not provided, uses timestamp)

set -e

JAR_FILE="${1}"
S3_BUCKET="${2}"
REGION="${3}"
VERSION="${4}"

if [ -z "$JAR_FILE" ] || [ -z "$S3_BUCKET" ] || [ -z "$REGION" ]; then
  echo "Usage: $0 <jar-file> <s3-bucket> <region> [version]"
  echo "  jar-file: Path to Lambda JAR file"
  echo "  s3-bucket: S3 bucket name"
  echo "  region: AWS region"
  echo "  version: Optional version (if not provided, uses timestamp)"
  exit 1
fi

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
  echo "❌ Error: JAR file not found: $JAR_FILE"
  exit 1
fi

# Extract version from tag if available, otherwise use timestamp
if [ -z "$VERSION" ]; then
  VERSION="$(date +%Y%m%d%H%M%S)"
fi

# Include version in S3 key to ensure CloudFormation detects changes
S3_KEY="lambda-packages/country-service-lambda-${VERSION}.jar"

echo "Uploading $JAR_FILE to s3://$S3_BUCKET/$S3_KEY"
aws s3 cp "$JAR_FILE" "s3://$S3_BUCKET/$S3_KEY" --region "$REGION"

echo "Uploaded to s3://$S3_BUCKET/$S3_KEY"
echo "S3 key includes version: $VERSION"
echo "$S3_KEY"

