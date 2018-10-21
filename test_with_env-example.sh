#!/usr/bin/env bash

# (only needed for integration tests)
# copy me to test_with_env.sh . fill out with variables and run for integration testing
# and then run with  `bash test_with_env.sh`

export PICO_TEST_BUCKET="some-bucket-name"
export PICO_TEST_PUBLIC_BUCKET="some-bucket-name"

export PICO_TEST_OBJECT="some-bucket-object"

export PICO_TEST_ACCESS_KEY="some-access-key"
export PICO_TEST_SECRET_KEY="some-secret-key"
export PICO_TEST_SESSION_TOKEN="some-session-token"


mvn test