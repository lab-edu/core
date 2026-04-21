package edu.lab.core.course.learning;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "lab.ai.openai")
public class AssignmentAiOpenAiProperties {

    private String baseUrl = "https://api.openai.com/v1";

    private String apiKey;

    private String model = "gpt-4.1-mini";

    @Min(5)
    private int timeoutSeconds = 40;
}
