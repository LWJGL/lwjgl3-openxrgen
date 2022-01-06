LWJGL 3 - OpenXR Template Generator
===================================

The LWJGL 3 OpenXR template generator is a tool that parses the OpenXR API
specification and generates LWJGL 3 Generator templates. The goal is to
fully automate the process of updating the LWJGL bindings of OpenXR and all
its extensions.

This repository is a port of [lwjgl3-vulkangen](https://github.com/LWJGL/lwjgl3-vulkangen).
The mechanics of parsing the corresponding `-Docs` repositories are almost identical.
Changes to this repo should be synced with `lwjgl3-vulkangen` and vice-versa.
This means extra work for the maintainers, but more flexibility in case the mechanics diverge.
If they prove stable, it should not be too difficult to merge the two repos and abstract the source/target differences.

Instructions
------------

- Clone [OpenXR-Docs](https://github.com/KhronosGroup/OpenXR-Docs.git).
- Apply the [LWJGL-fixes](https://github.com/LWJGL/lwjgl3-openxrgen/blob/master/LWJGL-fixes.patch) patch. (e.g. `patch -p1 < LWJGL-fixes.patch` under `<OpenXR-Docs>/`)
- Build the reference pages. All extensions should be included (e.g. `./makeAllExts -j 8 manhtmlpages` under `<OpenXR-Docs>/specification/`).
- Clone [LWJGL 3](https://github.com/LWJGL/lwjgl3.git).
- Set `path.openxr-docs` and `path.lwjgl3` in `pom.xml` to the root of the cloned OpenXR-Docs and LWJGL 3 repositories respectively.
- Run `mvn compile exec:java`.