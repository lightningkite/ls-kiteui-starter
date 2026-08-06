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
terraform {
  backend "s3" {
    bucket = "lightningkite-terraform"
    key = "lskiteuistarter/frontend"
    region = "us-west-2"
  }
}

provider "aws" {
  region = "us-west-2"
}
provider "aws" {
  alias  = "acm"
  region = "us-east-1"
}

module "web" {
  source = "github.com/lightningkite/terraform-static-site.git?ref=1.2.0"
  providers = {
    aws = aws
    aws.acm = aws.acm
  }
  deployment_name  = "lskiteuistarter"
  dist_folder = "../../build/vite/dist"
  domain_name      = "app.lskiteuistarter.cs.lightningkite.com"
  domain_name_zone = "cs.lightningkite.com"
  react_mode = true
}

