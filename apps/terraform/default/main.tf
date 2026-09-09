# by Claude — Terraform config template for S3 + CloudFront static web hosting.
# Customize the values below after forking the starter project.
#
# Prerequisites:
#   - AWS account with Route 53 hosted zone for your domain
#   - ACM certificate in us-east-1 (required for CloudFront)
#   - S3 bucket for Terraform state (or remove the backend block for local state)
#
# Usage:
#   cd apps/terraform/default
#   terraform init
#   terraform apply
#
# Or via Gradle:
#   ./gradlew :apps:deployWebdefault

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.53.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.8.0"
    }
  }
  required_version = "~> 1.0"
}

# TODO: Configure your Terraform state backend.
# Option 1: S3 backend (recommended for teams)
# terraform {
#   backend "s3" {
#     bucket = "your-terraform-state-bucket"
#     key    = "your-project/frontend"
#     region = "us-west-2"
#   }
# }
# Option 2: Remove this block entirely to use local state (fine for solo dev).

provider "aws" {
  region = "us-west-2"  # TODO: Change to your preferred region
}

provider "aws" {
  alias  = "acm"
  region = "us-east-1"  # ACM certs for CloudFront must be in us-east-1
}

module "web" {
  source = "github.com/lightningkite/terraform-static-site.git?ref=1.2.0"
  providers = {
    aws     = aws
    aws.acm = aws.acm
  }

  # TODO: Customize these for your project
  deployment_name  = "myproject"                        # S3 bucket name prefix
  dist_folder      = "../../build/vite/dist"            # Path to Vite build output
  domain_name      = "app.myproject.example.com"        # Your app's domain
  domain_name_zone = "example.com"                      # Route 53 hosted zone
  react_mode       = true                               # SPA routing (index.html fallback)
}
