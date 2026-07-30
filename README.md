## Building AI agents with Spring AI 2.0 backboned via Koog AI framework 

When building AI agent programmer's mindset needs to be:

1) There is a problem to solve
2) And there needs to be a set of tools solving it

Everything else is directed by LLM (AI).

# What Koog framework adds to it

When asking the LLM prompt to solve something then result is unpredictable. Basically you are having just a hope.
**With [Koog](https://docs.koog.ai/) you are giving the LLM the guardrails, the contract (your tools - code)** of what needs to be done to solve your problem. 

Imagine you want a chat AI agent sitting in the front of the customer
which eventually calls bank account eligibility AI agent (containing your business code) once he has complete customer's data:

<img width="1733" height="1637" alt="image" src="https://github.com/user-attachments/assets/576be73a-b8bb-4d53-986c-8f591787b1b9" />

Person eligibility agent is doing three checks:

    - accountCheck (does customer already have requested account?...etc)
    - debtCheck (isn't customer in the debt registries?)
    - personCheck (isn't customer too old?)

all of these checks are tools described via LLMDescription annotation so LLM is able to use them correctly. 
See: [`createStrategy`](https://github.com/tomaskloucek272/spring-ai-koog/blob/f632bb0f861c0c2b29d99101cb8c524bb31669f4/src/main/java/com/example/ai_koog/service/AgentService.java#L96)

# Running the demo

Demo uses OPENAI as the LLM so you need to have account at [OPENAI platform](https://platform.openai.com/) and then 
you need to generate the API Key. After that to compile and build:

    mvn clean install

and to start it:

    OPENAI_API_KEY=<YOUR_API_KEY> java -jar target/ai-koog-0.0.1-SNAPSHOT.jar

App uses [Spring AI Koog spring boot starter](https://blog.jetbrains.com/ai/2026/04/introducing-koog-integration-for-spring-ai-smarter-orchestration-for-your-agents/) which autowires LLM prompt for us so you should see this after start:

<img width="2326" height="741" alt="image" src="https://github.com/user-attachments/assets/20c1b61d-603a-429f-b4aa-1d153d74c9ed" />

# Testing the AI Agents

    curl -X POST http://localhost:8080/api/agent/messages -H "Content-Type: application/json" -d '{"message": "Hi, tell me who you are!"}'

Response:

    I’m an AI assistant that can help answer questions and, in this chat, I can also run account eligibility checks if you provide two details: 
    your **subjectIdentifier** and the **accountType** you’re applying for.
















