(defproject elb-log-tool "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojure/core.async "0.2.374"]
                 [com.cemerick/url "0.1.1"]
                 [http-kit "2.1.19"]
                 [clj-time "0.11.0"]
                 [prismatic/schema "1.1.1"]
                 [camel-snake-kebab "0.4.0"]
                 [org.postgresql/postgresql "9.4.1208"]

                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]
                 [ch.qos.logback/logback-classic "1.1.7"]

                 [amazonica "0.3.57" :exclusions [com.amazonaws/aws-java-sdk-api-gateway
                                                  com.amazonaws/aws-java-sdk-autoscaling
                                                  com.amazonaws/aws-java-sdk-cloudformation
                                                  com.amazonaws/aws-java-sdk-cloudfront
                                                  com.amazonaws/aws-java-sdk-cloudhsm
                                                  com.amazonaws/aws-java-sdk-cloudtrail
                                                  com.amazonaws/aws-java-sdk-cloudwatch
                                                  com.amazonaws/aws-java-sdk-cloudwatchmetrics
                                                  com.amazonaws/aws-java-sdk-codecommit
                                                  com.amazonaws/aws-java-sdk-codedeploy
                                                  com.amazonaws/aws-java-sdk-codepipeline
                                                  com.amazonaws/aws-java-sdk-cognitoidentity
                                                  com.amazonaws/aws-java-sdk-cognitosync
                                                  com.amazonaws/aws-java-sdk-config
                                                  com.amazonaws/aws-java-sdk-datapipeline
                                                  com.amazonaws/aws-java-sdk-devicefarm
                                                  com.amazonaws/aws-java-sdk-directconnect
                                                  com.amazonaws/aws-java-sdk-directory
                                                  com.amazonaws/aws-java-sdk-dynamodb
                                                  com.amazonaws/aws-java-sdk-ecr
                                                  com.amazonaws/aws-java-sdk-ecs
                                                  com.amazonaws/aws-java-sdk-efs
                                                  com.amazonaws/aws-java-sdk-elasticache
                                                  com.amazonaws/aws-java-sdk-elasticbeanstalk
                                                  com.amazonaws/aws-java-sdk-elasticsearch
                                                  com.amazonaws/aws-java-sdk-elastictranscoder
                                                  com.amazonaws/aws-java-sdk-emr
                                                  com.amazonaws/aws-java-sdk-glacier
                                                  com.amazonaws/aws-java-sdk-importexport
                                                  com.amazonaws/aws-java-sdk-inspector
                                                  com.amazonaws/aws-java-sdk-iot
                                                  com.amazonaws/aws-java-sdk-kinesis
                                                  com.amazonaws/aws-java-sdk-machinelearning
                                                  com.amazonaws/aws-java-sdk-marketplacecommerceanalytics
                                                  com.amazonaws/aws-java-sdk-opsworks
                                                  com.amazonaws/aws-java-sdk-rds
                                                  com.amazonaws/aws-java-sdk-redshift
                                                  com.amazonaws/aws-java-sdk-route53
                                                  com.amazonaws/aws-java-sdk-ses
                                                  com.amazonaws/aws-java-sdk-simpledb
                                                  com.amazonaws/aws-java-sdk-simpleworkflow
                                                  com.amazonaws/aws-java-sdk-sns
                                                  com.amazonaws/aws-java-sdk-sqs
                                                  com.amazonaws/aws-java-sdk-ssm
                                                  com.amazonaws/aws-java-sdk-storagegateway
                                                  com.amazonaws/aws-java-sdk-sts
                                                  com.amazonaws/aws-java-sdk-support
                                                  com.amazonaws/aws-java-sdk-swf-libraries
                                                  com.amazonaws/aws-java-sdk-waf
                                                  com.amazonaws/aws-java-sdk-workspaces]]])
