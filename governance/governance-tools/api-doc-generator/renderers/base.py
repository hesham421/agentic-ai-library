"""
Renderer interface. A renderer's only job is turning an already-fully-built
ApiDocument into output files — it must not extract or infer anything itself.
Extraction and rendering are deliberately separate so new output formats
(OpenAPI YAML, Postman/Insomnia collections, frontend model stubs, ...) can
be added later as new Renderer subclasses without touching any extractor.
"""

from abc import ABC, abstractmethod

from models.api_doc_model import ApiDocument


class Renderer(ABC):

    @abstractmethod
    def render(self, document: ApiDocument) -> dict[str, str]:
        """Returns {relative_output_path: file_content}. Callers write these
        under --output; the renderer itself never touches the filesystem."""
        raise NotImplementedError
