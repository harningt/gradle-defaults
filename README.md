# us.eharning.gradle.defaults

[![License](http://img.shields.io/badge/license-Apache_2-red.svg)][Apache2.0]

This gradle plugin implements a set of defaults to be used by various projects
under us.eharning.

## Versioning

This library will follow the guidelines set forth in [Semantic Versioning 2.0][SemVer2.0]

Public APIs not marked with @Beta are considered under the purview of the versioning rules.

@Beta items follow the attached documentation to the annotation, shortly put:

> Do not rely on this to exist in the future as it is not "API-frozen".
> It may change functionality or be removed in any future release.

Public APIs underneath any 'internal' namespace namespace are not
considered 'public' per the versioning rules.

## License

This library is covered under the [Apache 2.0 license][Apache2.0] as indicated in the LICENSE file.

## Repository Details

The repository is managed using the Gitflow workflow. Note that any published
feature/* branches are subject to history modification, so beware working
off of those.

Non-annotated tags will be added in the form vMAJOR.MINOR.MICRO-DEV to denote the
start of a new feature. This will guide the next release to be versioned as
vMAJOR.MINOR.MICRO. Without this, the next expected version would be a MICRO-change.

Signed and annotated tags will be added in the form vMAJOR.MINOR.MICRO to denote
releases.

[Apache2.0]: http://www.apache.org/licenses/LICENSE-2.0
[SemVer2.0]: http://semver.org/spec/v2.0.0.html
