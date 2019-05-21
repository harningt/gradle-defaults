# Procedure for Releasing
## Test locally using SNAPSHOT

Minor release:
```./gradlew publishToMavenLocal```

Alternate release:
```./gradlew publishToMavenLocal -P reckon.scope=major```
```./gradlew publishToMavenLocal -P reckon.scope=patch```

## git-flow release steps

```
git flow release start ${VERSION}
git flow release finish -s
```

## Publishing

```
git checkout v${VERSION}
./gradlew clean assemble publishPlugins
```
