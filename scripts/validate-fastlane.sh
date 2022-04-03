#!/bin/bash
docker run --rm --workdir /app --mount type=bind,source="$(pwd)",target=/app    ashutoshgngwr/validate-fastlane-supply-metadata:v1
