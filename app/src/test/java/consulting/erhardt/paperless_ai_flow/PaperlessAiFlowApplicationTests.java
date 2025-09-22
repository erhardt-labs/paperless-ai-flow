package consulting.erhardt.paperless_ai_flow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
	classes = consulting.erhardt.paperless_ai_flow.app.PaperlessAiFlowApplication.class,
	properties = {
		"paperless.api.base-url=http://localhost:8000",
		"paperless.api.token=test-token",
		"spring.ai.openai.api-key=test-key",
		"spring.integration.poller.default.fixed-delay=1000",
		"paperless.pipelines[0].name=test-pipeline",
		"paperless.pipelines[0].selector.required-tags[0]=inbox",
		"paperless.pipelines[0].polling.interval=PT10S",
		"paperless.pipelines[0].polling.enabled=false",
		"paperless.pipelines[0].ocr.model=openai/gpt-4o",
		"paperless.pipelines[0].ocr.prompt=Extract text from this document"
	}
)
class PaperlessAiFlowApplicationTests {

	@Test
	void contextLoads() {
	}

}
