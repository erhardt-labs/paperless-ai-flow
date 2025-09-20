# Project Brief: Paperless-AI Pipeline

## Overview
The **Paperless-AI Pipeline** is a headless, YAML-configurable service for automated document processing in [Paperless-ngx](https://github.com/paperless-ngx/paperless-ngx). Unlike existing tools, it supports **multi-step pipelines** (OCR + AI enrichment + metadata updates) and allows multiple **source-specific processing profiles** to be defined declaratively in YAML.

## Core Requirements

### Functional Requirements
1. **Document polling:** Query Paperless-ngx for documents matching source selectors (tags, filename regex, etc.)
2. **OCR processing:** Convert PDFs to images and extract text via OCR providers
3. **AI enrichment:** Extract metadata (title, tags, correspondent, custom fields) using LLM prompts defined in YAML
4. **Normalization:** Apply configurable alias mappings and tag normalizations before updating Paperless
5. **Update Paperless:** Write enriched metadata back via the Paperless API
6. **Configuration via YAML:** All sources, providers, and prompts configurable via YAML files
7. **Idempotency:** Process each document once per pipeline version unless reprocessing required

### Non-Functional Requirements
- **Reliability:** At-least-once processing with retry and backoff
- **Extensibility:** Easy addition of new OCR/LLM providers or processing steps
- **Performance:** Efficient polling with parallelization across sources
- **Security:** API tokens/keys from environment variables, no sensitive values in YAML
- **Observability:** Structured logging with correlation IDs, metrics, health endpoints
- **Deployment:** Container-packaged for Docker/Kubernetes with mounted configuration

## Key Features
- **YAML-driven configuration** with multiple source rules and pipeline steps
- **Pipeline support** for modular, reusable processing chains
- **Multiple providers** for OCR (Tesseract, Google Vision, Azure OCR, ABBYY) and LLM (OpenAI, Ollama, Azure OpenAI)
- **AI-driven enrichment** with structured JSON outputs validated against schema
- **Idempotent processing** with version and hash tracking
- **Headless & cloud-native** microservice architecture

## Example Workflow
1. Pipeline polls Paperless for documents tagged with `OCR_QUEUE`
2. Matching PDF downloaded and converted to images
3. OCR text extracted using configured provider
4. OCR text passed to LLM with YAML-defined prompts
5. Normalization maps aliases and canonicalizes tags
6. Paperless updated with enriched metadata and `PROCESSED` tag applied
7. Document skipped in future runs due to stored hash and pipeline version

## Success Criteria
- Automated processing of documents without manual intervention
- Reliable extraction and enrichment of metadata
- Seamless integration with existing Paperless-ngx installations
- Easy configuration and deployment for various use cases
- Extensible architecture for future enhancements
