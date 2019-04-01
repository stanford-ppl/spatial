#!/bin/bash

sbt ";project model; set mainClass in assembly := Some(\"model.AppRuntimeModel_dse\"); assembly"
