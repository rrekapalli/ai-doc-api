## Refactor to use latest AI Tools

## Concept

AI-Doc-API is a comprehensive AI-powered health tracking application's backend service built with Java (v21) + Spring Boot + Spring AI and other related technologies that provide REST API end points to Hi-Doc Flutter based front end applciation that users can use to manage their health data, medications, activities, etc and receive AI-powered health insights. Currenlty this is implemented without using any advanced features of Spring AI like @Tool annotations, Chat Orchestrators, Advisors, Chat Memory and Conversation history etc. This refacotr should:
- Update the existing infrastrucutre to enable latest technology integration like Spring AI with @Tool annotations, Chat Orchestrators, Advisors, Chat Memory and Conversation history
- MUST use existing infra
- MUST provide same existing REST endpoints in addition to any new end points as may be required 
- MUST USE prompts provided in `../resources/prompts` for parsing various health related messgaes that user can send as messages and send the response in a specific format (improvise these prompts if required)
- MUST support conversational style message processing based on previous chat history and current user input
- MUST implement a central chat orchestator that can facilitate conversation chat that can classify user messages and re-direct to appropriate AI tool for further processing and response generation (using @Tool annotations). Example classifications can be the following (but not limited to) and any other health related user interactable classifications:
    - Medication Data Entry
    - Acitivity Data Entry
    - Health Condition Data Entry
    - Food intake
    - Mood Data Entry
    - Health Parameter Data Entry
    - Medical Queries (like symptoms, side effects, human physiology, anatomy etc)
    - Report Parsing
    - Trend of health patterns based on all past history and above data
- MUST reject any general and queries un related to health with a gentle and cordial messag (can be impleneted as a generic @Tool) and this need not be deducted from rate limit 
- If the user message is related to trend analysis, or historic data request of his/her past health data, then the system can just send the response with classifier 'TREND_ANALYSIS' and the the response can include the category of data that is requested. As the complete user health data (along with messages and all) is going to be stored in an SQLite database on client device, and is not accessible for this API service, this information should help the client front end application to determine what kind of data to ull from that local datbase. The inference can suggest if the user request is for mere dispaly of data or requesting like a chart or a followup is needed
- Current AIResponse can be updated to include the following (if missing) to generalize the JSON structure like below (imporvise if required):
    {
        classification: 'HEALTH_PARAM' | 'MEDICATION_ENTRY' | 'ACIVITY'  etc.,
        response: <AI response in HTML>,
        inference: <AI inference text (if applciable)>,
        data: <A JSON data response for tables or charts>,
        dateTime: <date time of response>,
        messageId: <UUID of message>,
        conversationId: <UUID of conversation>,
        userId: <UUID of user>,
        availableRequests: <How many requests available after the current one deduncated from rate limit>,
        isFollowUp: <True|False>,
        followUpDataRequired: <HISTORIC|HEALTH_PARAM|MEDICATION_ENTRY|ACTIVITY etc>
    }
- UPDATE AIResponse to include 'availableRequests' to show how many request are left based on the rate limit value
- UPDATE the existing AIRequest to include messageHistory field that can contain last 'N' (configurable, default can be 100) user messages
- MUST provide a bare minimum implementation. Make all other infra related tasks (like Telemetry, Observablity, caching, comprehensive testing etc) as optional so that they can be impleneted later
- There shall be a separate @Tool for 'prognosis', which should take all historic data, a separate sub-object for each category like HEALTH_PARAM, MEDICATION_ENTRY, ACTIVITY etc, that should be sent to AI system to get a compelte prognosis of the user based on that historic data. This should be determined from the user intent and may be related to TREND_ANALYSIS, so clearly identiy the intent and the TREND_ANALYSIS response can include a followup message in the response too (with PROGNOSIS as classification - front end should handle this clasisfication message by sending historic data as the above explained format). Once the followup data is provided to the AI system, it should generate a comprehensive prognosis response based on the histroic data provided