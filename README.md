## Building AI agents with Spring AI 2.0 backboned via Koog AI framework 

When building AI agent programmer's mindset needs to be:

1) There is a problem to solve
2) And there needs to be a set of tools solving it

Everything else is directed by LLM (AI).

# What Koog framework adds to it

When asking the LLM prompt to solve something then result is unpredictable. Basically you are having just a hope.
**With Koog you are giving the LLM the guardrails, the contract (your code)** of what needs to be done to solve your problem. 

Imagine you want a chat AI agent which eventually calls bank account eligibility AI agent (containing your business code):

<img width="1733" height="1637" alt="image" src="https://github.com/user-attachments/assets/576be73a-b8bb-4d53-986c-8f591787b1b9" />

Person eligibility agent is doing three checks:

- accountCheck (does customer already have requested account?...etc)
- debtCheck (isn't customer in the debt registries?)
- personCheck (isn't customer too old?)

all of these checks are tools described via LLMDescription annotation so LLM is able to use them correctly. 
See: [`createStrategy`](https://github.com/OWNER/REPO/blob/main/src/main/java/com/example/ai_koog/service/AgentService.java#L96-L239)

# Running the demo

Demo uses OPENAI as the LLM so you need to have account at [OPENAI platform](https://platform.openai.com/)
and then you need to generate the API Key. After that:

    mvn clean install


















