# PhoneBridge Alexa Custom Skill

This directory contains the Amazon Alexa Custom Skill configuration, AWS Lambda code, and Interaction Model for **PhoneBridge**.

## Structure

```
alexa-skill/
├── lambda/                    # AWS Lambda function source code
│   ├── index.js               # Main Alexa skill intent handlers (ask-sdk-core)
│   ├── package.json           # ask-sdk-core dependencies
│   └── services/
│       └── backendClient.js   # HTTP client communicating with PhoneBridge backend
├── interaction-model/
│   ├── en-IN.json             # English (India) interaction model
│   └── en-US.json             # English (US) interaction model
├── alexa-interaction-model.json # Combined interaction model schema
├── test/
│   └── skill.test.js          # Unit tests verifying all intent responses
└── package.json
```

## Supported Voice Commands & Utterances

- **Query Caller**:
  - *"Alexa, ask Phone Bridge who is calling"*
  - *"Alexa, ask Phone Bridge who is calling me"*
  - Response: *"You have an incoming call from Rahul."* or *"There is no incoming call right now."*

- **Answer Call**:
  - *"Alexa, ask Phone Bridge to answer"*
  - *"Alexa, ask Phone Bridge to pick up"*
  - Response: *"Okay, I answered the call."* or error explanation if Android restricts background answering.

- **Reject Call**:
  - *"Alexa, ask Phone Bridge to reject"*
  - *"Alexa, ask Phone Bridge to decline the call"*
  - Response: *"Okay, I rejected the call."* or error explanation.

- **Check Call Status**:
  - *"Alexa, ask Phone Bridge for call status"*
  - *"Alexa, ask Phone Bridge to check my phone"*

## Environment Variables for Lambda

When deploying the Lambda function to AWS:

| Variable | Description | Example |
|---|---|---|
| `BACKEND_URL` | Public HTTPS URL of the PhoneBridge Backend | `https://xyz.ngrok-free.app` |
| `ALEXA_CLIENT_SECRET` | Secret token matching backend `ALEXA_CLIENT_SECRET` | `alexa_skill_secret_456` |
| `BACKEND_TIMEOUT_MS` | Timeout for backend HTTP requests (default 5000ms) | `5000` |

## Deployment Guide

Refer to [docs/ALEXA_SETUP.md](../../docs/ALEXA_SETUP.md) for full step-by-step instructions on configuring the skill in the Amazon Developer Console and AWS Lambda.
