pipeline {
   agent {
    label 'Docker&&Agent'
  }
  environment {
    JAVA_HOME = '/docker-java-home'
    MAVEN_HOME = '/usr/share/maven'
  }

  options { buildDiscarder(logRotator(numToKeepStr: '10')) }

  stages {
    stage('Setup Agent') {
      agent {
        docker {
          reuseNode true
          image "${nexus.dockerPullRepository()}/agent/java/1.11.0"
        }
      }
      steps {
        script {
          jenkinsAgentBuildJava {
            projectName = 'shared'
            componentName = 'ms-html-to-pdfa'
            slackChannel = 'health-pdu-fhajenkins'
            dailyBuildEnabled = false
            buildGoals = [
              "org.jacoco:jacoco-maven-plugin:prepare-agent"
            ]
          }
        }
      }
    }
    stage('Dockerize') {
      steps {
        script {
          jenkinsAgentDockerBuild {
            projectName = "integn"
            componentName = "ms-html-to-pdfa"
            slackChannel = 'health-pdu-fhajenkins'
          }
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}
