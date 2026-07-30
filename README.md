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

First let's test chat agent before us:

    curl -X POST http://localhost:8080/api/agent/messages \
    -H "Content-Type: application/json" \
    -d '{"message": "Hi, tell me who you are!"}'

Response:

    I’m an AI assistant that can help answer questions and, in this chat, I can also run account eligibility checks if you provide two details: 
    your **subjectIdentifier** and the **accountType** you’re applying for.

Next question:

    curl -X POST http://localhost:8080/api/agent/messages \
    -H "Content-Type: application/json" \
    -d '{"message": "Do you offer some other services?"}'

Response:

    Yes. Common services many banks/financial providers offer (besides opening accounts) include:

    - Cards: debit/credit cards, virtual cards, card controls (freeze/unfreeze), chargebacks
    - Payments: domestic/international transfers, scheduled payments, direct debits, bill pay
    - Cash services: ATM withdrawals, cash deposits (where supported), foreign currency
    - Savings & investing: savings goals, term deposits, funds/ETFs, brokerage (if available)
    - Lending: personal loans, overdrafts/credit lines, mortgages, refinancing
    - Digital features: mobile app, budgeting insights, alerts, spending categories, statements
    - Security: 2FA, transaction notifications, fraud monitoring
    - Business services (if applicable): business accounts, invoicing, expense cards, payroll

    If you tell me what you’re trying to do (e.g., “send money abroad” or “get a credit card”), I can narrow it down.

Now testing the eligibility agent sitting behind:

    curl -X POST http://localhost:8080/api/agent/messages \
    -H "Content-Type: application/json" \
    -d '{"message": "Okay, my name is Tomas Kloucek, my subjectIdentifier is 991231/0099 and I want saving account"}'

Now chat agent invoked person eligibility agent **calling our business code**:

    2026-07-30T15:12:28.162+02:00  INFO 94077 --- [ai-koog] [atcher-worker-1] ai.koog.agents.core.agent.GraphAIAgent   : (agent id: 4cab413b-b0ea-4512-ab4f-34a1d77f7ee9) Executing tool (name:       eligibilityCheck, args: {"input":{"subjectIdentifier":"991231/0099","accountType":"SAVINGS"}}
    2026-07-30T15:12:28.168+02:00  INFO 94077 --- [ai-koog] [atcher-worker-1] a.k.a.c.a.entity.AIAgentSubgraphBase     : No enforced execution point, starting from __start__ [graphStrategy,         graphStrategy, 4e8d1728-8d87-4f1b-9d97-129b9a70cc58]
    2026-07-30T15:12:28.170+02:00  INFO 94077 --- [ai-koog] [atcher-worker-1] a.k.a.c.a.entity.AIAgentSubgraphBase     : No enforced execution point, starting from __start__ [subgraph,              graphStrategy, 4e8d1728-8d87-4f1b-9d97-129b9a70cc58] 
    2026-07-30T15:12:34.116+02:00  INFO 94077 --- [ai-koog] [atcher-worker-1] ai.koog.agents.core.agent.GraphAIAgent   : (agent id: null.0) Executing tool (name: checkAccount, args:                 {"subjectIdentifier":"991231/0099","accountType":"SAVINGS"}
    2026-07-30T15:12:36.644+02:00  INFO 94077 --- [ai-koog] [atcher-worker-1] a.k.a.c.a.entity.AIAgentSubgraphBase     : No enforced execution point, starting from __start__ [subgraph,              graphStrategy, 4e8d1728-8d87-4f1b-9d97-129b9a70cc58]
    2026-07-30T15:12:48.320+02:00  INFO 94077 --- [ai-koog] [atcher-worker-2] ai.koog.agents.core.agent.GraphAIAgent   : (agent id: null.0) Executing tool (name: checkSolus, args:            
    {"subjectIdentifier":"991231/0099"}
    2026-07-30T15:12:50.184+02:00  INFO 94077 --- [ai-koog] [atcher-worker-2] a.k.a.c.a.entity.AIAgentSubgraphBase     : No enforced execution point, starting from __start__ [subgraph,         
    graphStrategy, 4e8d1728-8d87-4f1b-9d97-129b9a70cc58]
    2026-07-30T15:12:54.636+02:00  INFO 94077 --- [ai-koog] [atcher-worker-2] ai.koog.agents.core.agent.GraphAIAgent   : (agent id: null.0) Executing tool (name: checkApplicant, args:       
    {"subjectIdentifier":"991231/0099"}

     Tool call trace

      ┌──────────────┬──────────────────┬──────────┬───────────────────────────────────────────────────────────────────────┐
      │ Time (UTC+2) │       Tool       │  Worker  │                                 Args                                  │
      ├──────────────┼──────────────────┼──────────┼───────────────────────────────────────────────────────────────────────┤
      │ 15:12:28.162 │ eligibilityCheck │ worker-1 │ {"input":{"subjectIdentifier":"991231/0099","accountType":"SAVINGS"}} │
      ├──────────────┼──────────────────┼──────────┼───────────────────────────────────────────────────────────────────────┤
      │ 15:12:34.116 │ checkAccount     │ worker-1 │ {"subjectIdentifier":"991231/0099","accountType":"SAVINGS"}           │
      ├──────────────┼──────────────────┼──────────┼───────────────────────────────────────────────────────────────────────┤
      │ 15:12:48.320 │ checkSolus       │ worker-2 │ {"subjectIdentifier":"991231/0099"}                                   │
      ├──────────────┼──────────────────┼──────────┼───────────────────────────────────────────────────────────────────────┤
      │ 15:12:54.636 │ checkApplicant   │ worker-2 │ {"subjectIdentifier":"991231/0099"}                                   │
      └──────────────┴──────────────────┴──────────┴───────────────────────────────────────────────────────────────────────┘
Response to chat agent / customer:

    {"subjectIdentifier":"991231/0099","eligible":true,"rejectionReason":"NONE","rejectionDetail":null,"checkedAt":"2026-07-30T15:12:56.064499"}

Exactly as I instructed him:

    















