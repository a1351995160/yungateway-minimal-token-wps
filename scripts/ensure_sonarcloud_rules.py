#!/usr/bin/env python3
"""Ensure required SonarCloud Java rules are active for this project."""

import base64
import json
import os
import urllib.error
import urllib.parse
import urllib.request


SONARCLOUD_URL = "https://sonarcloud.io"
JAVA_LANGUAGE = "java"
PROFILE_NAME = "Yundoc Java CI"
REQUIRED_RULES = ("javabugs:S2259", "java:S2699")


def main():
    token = require_env("SONAR_TOKEN")
    organization = require_env("SONAR_ORGANIZATION")
    project_key = require_env("SONAR_PROJECT_KEY")
    client = SonarCloudClient(token)

    current_profile = current_java_profile(client, organization, project_key)
    profile = ensure_project_profile(client, organization, project_key, current_profile)
    for rule in REQUIRED_RULES:
        ensure_rule_active(client, organization, profile["key"], rule)

    print(
        "SonarCloud Java profile '{0}' is associated with '{1}', required rules are active: {2}".format(
            profile["name"], project_key, ", ".join(REQUIRED_RULES)
        )
    )


def require_env(name):
    value = os.environ.get(name)
    if value:
        return value
    raise SystemExit("{0} is required to configure SonarCloud quality profile rules.".format(name))


def current_java_profile(client, organization, project_key):
    response = client.get(
        "/api/qualityprofiles/search",
        organization=organization,
        project=project_key,
        language=JAVA_LANGUAGE,
    )
    profiles = response.get("profiles", [])
    if not profiles:
        raise SystemExit("No SonarCloud Java quality profile is associated with {0}.".format(project_key))
    return profiles[0]


def ensure_project_profile(client, organization, project_key, current_profile):
    if not current_profile.get("isBuiltIn") and current_profile.get("name") == PROFILE_NAME:
        return current_profile

    profile = find_profile(client, organization, PROFILE_NAME)
    if profile is None:
        profile = client.post(
            "/api/qualityprofiles/copy",
            fromKey=current_profile["key"],
            toName=PROFILE_NAME,
        ).get("profile")

    if profile is None or "key" not in profile:
        raise SystemExit("Unable to create or find SonarCloud Java quality profile '{0}'.".format(PROFILE_NAME))

    if current_profile.get("key") != profile["key"]:
        client.post(
            "/api/qualityprofiles/add_project",
            organization=organization,
            language=JAVA_LANGUAGE,
            project=project_key,
            qualityProfile=profile["name"],
        )
    return profile


def find_profile(client, organization, profile_name):
    response = client.get(
        "/api/qualityprofiles/search",
        organization=organization,
        language=JAVA_LANGUAGE,
    )
    for profile in response.get("profiles", []):
        if profile.get("name") == profile_name:
            return profile
    return None


def ensure_rule_active(client, organization, profile_key, rule_key):
    if is_rule_active(client, organization, profile_key, rule_key):
        return
    client.post(
        "/api/qualityprofiles/activate_rule",
        key=profile_key,
        rule=rule_key,
    )
    if not is_rule_active(client, organization, profile_key, rule_key):
        raise SystemExit("SonarCloud rule {0} was not activated for profile {1}.".format(rule_key, profile_key))


def is_rule_active(client, organization, profile_key, rule_key):
    response = client.get("/api/rules/show", organization=organization, key=rule_key)
    for active in response.get("actives", []):
        if active.get("qProfile") == profile_key:
            return True
    return False


class SonarCloudClient:
    def __init__(self, token):
        credentials = base64.b64encode(("{0}:".format(token)).encode("utf-8")).decode("ascii")
        self.headers = {"Authorization": "Basic {0}".format(credentials)}

    def get(self, path, **params):
        query = urllib.parse.urlencode(params)
        return self.request("GET", "{0}{1}?{2}".format(SONARCLOUD_URL, path, query))

    def post(self, path, **params):
        body = urllib.parse.urlencode(params).encode("utf-8")
        headers = dict(self.headers)
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        return self.request("POST", "{0}{1}".format(SONARCLOUD_URL, path), body, headers)

    def request(self, method, url, body=None, headers=None):
        request = urllib.request.Request(url, data=body, headers=headers or self.headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                payload = response.read().decode("utf-8")
        except urllib.error.HTTPError as error:
            message = error.read().decode("utf-8", errors="replace")
            raise SystemExit("SonarCloud API request failed: {0} {1}".format(error.code, message))
        except urllib.error.URLError as error:
            raise SystemExit("SonarCloud API request failed: {0}".format(error.reason))
        if not payload:
            return {}
        return json.loads(payload)


if __name__ == "__main__":
    main()
