package consulting.erhardt.paperless_ai_flow;

import consulting.erhardt.paperless_ai_flow.ai.ocr.OcrClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CustomFieldsService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.DocumentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatModel;

/**
 * Abstract base class for service unit tests.
 * Provides common mocks for services that interact with external dependencies.
 */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractServiceTest {

  @Mock
  protected OpenAiChatModel openAiChatModel;

  @Mock
  protected DocumentService documentService;

  @Mock
  protected TagService tagService;

  @Mock
  protected CorrespondentService correspondentService;

  @Mock
  protected CustomFieldsService customFieldsService;

  @Mock
  protected OcrClient ocrClient;
}
