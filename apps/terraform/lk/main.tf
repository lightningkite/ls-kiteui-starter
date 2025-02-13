terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.2.0"
    }
  }
  required_version = "~> 1.0"
}
terraform {
  backend "s3" {
    bucket = "lightningkite-terraform"
    key    = "blackstone-customer-support"
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
  source    = "git::https://github.com/lightningkite/terraform-static-site.git?ref=wasm-content-type"
  #  source    = "github.com/lightningkite/terraform-static-site.git"
#  content_security_policy = "frame-ancestors 'self'; default-src 'self'; img-src data: blob: ${var.external_media_sources}; media-src data: blob: ${var.external_media_sources}; script-src 'self' ${var.external_script_sources}; style-src 'self' 'unsafe-inline'; object-src 'none'; connect-src ${var.external_connections}"

  providers = {
    aws     = aws
    aws.acm = aws.acm
  }
  deployment_name         = "blackstone-customer-support"
  dist_folder             = "../../build/vite/dist"
  domain_name             = "blackstonesupport.cs.lightningkite.com"
  domain_name_zone        = "cs.lightningkite.com"
  react_mode              = true
}