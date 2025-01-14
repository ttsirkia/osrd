<p align="center">
  <a href="https://osrd.fr/en/">
    <img src="assets/branding/osrd_small.svg" width="200px" alt="OSRD logo"/>
  </a>
  <a href="https://publiccode.eu/">
    <img src="assets/PMPC_badge.svg" width="200px" alt="Public Money Public Code"/>
  </a>
</p>

<p align="center">
  <a href="https://dgexsolutions.github.io/osrd-website/en/developers/contribute/"><img src="https://img.shields.io/github/contributors-anon/DGEXSolutions/osrd" alt="Contributors badge" /></a>
  <a href="https://github.com/DGEXSolutions/osrd/blob/dev/LICENSE"><img src="https://img.shields.io/badge/license-LGPL-blue.svg" alt="LGPL License badge" /></a>
  <a href="https://github.com/DGEXSolutions/osrd/actions/workflows/integration_tests.yml"><img src="https://github.com/DGEXSolutions/osrd/actions/workflows/integration_tests.yml/badge.svg" alt="Integration Status" /></a>
</p>

## What is OSRD?

OSRD is an open source web application for railway infrastructure design,
capacity analysis, timetabling and simulation.

It's free and open-source forever!

## WARNING

OSRD it not yet production ready.
User and programming interfaces can and will change (now is the time to make suggestions!).
Important features are missing. Documentation is sparse.
Please don't rely on OSRD unless you are prepared to deal with frequent changes.

## Getting Started

To compile and run the application with an example infrastructure:

```sh
# build and run the entire stack
docker-compose up -d --build

# generate and load an example infrastructure
python3 -m venv .venv
source .venv/bin/activate
pip install geojson-pydantic
scripts/generate-infra.sh small_infra

# open the web app
xdg-open http://localhost:3000/
```

## Get in touch

- Chat with us on IRC at [libera.chat#osrd](https://web.libera.chat/#osrd)
- Email us at <contact@osrd.fr>

## Sponsors

<p align="center">
  <img src="assets/sponsors/france-dot.svg" width="150px" height="150px" alt="Ministère chargé des Transports"/>
  <img src="assets/sponsors/european-union.svg" width="150px" height="150px" alt="European Union"/>
  <img src="assets/sponsors/sncf-reseau.svg" width="150px" height="150px" alt="SNCF Réseau"/>
</p>
