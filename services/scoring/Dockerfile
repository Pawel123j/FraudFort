# FraudFort scoring service — FastAPI on uv.
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app \
    UV_NATIVE_TLS=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy

# uv binary, pinned, from its official image.
COPY --from=ghcr.io/astral-sh/uv:0.9 /uv /usr/local/bin/uv

WORKDIR /app

# Resolve+install runtime deps first so this layer is cached across code changes.
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev

COPY app ./app

# Proces bez uprawnień roota (FF-04).
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin scoring \
    && chown -R 10001:10001 /app
USER 10001:10001

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health').read()"

CMD ["uv", "run", "--frozen", "--no-dev", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
