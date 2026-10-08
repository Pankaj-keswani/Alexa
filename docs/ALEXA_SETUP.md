# Alexa Skill Setup & Developer Console Guide

This guide provides a comprehensive, beginner-friendly walkthrough to create, configure, and test the **PhoneBridge** Alexa Custom Skill in the **Amazon Developer Console**.

---

## Step 1: Sign In to Amazon Developer Console
1. Navigate to [developer.amazon.com/alexa](https://developer.amazon.com/alexa).
2. Sign in with your Amazon account (ideally the **same account** registered to your Echo / Alexa app).
3. In the top navigation bar, click **Developer Console** and choose **Alexa Skills Kit**.

---

## Step 2: Create a New Skill
1. Click the blue **Create Skill** button.
2. **Skill Name**: Enter `PhoneBridge`.
3. **Primary Locale**:
   - For India: Choose **English (IN)**.
   - For US: Choose **English (US)**.
4. **Choose a type of experience**: Select **Other > Custom**.
5. **Choose a hosting service**:
   - Select **Provision your own** (if deploying code to your own AWS Lambda or HTTPS server).
   - *Or* select **Alexa-hosted (Node.js)** if you want Amazon to host your Lambda for free!
6. Click **Next** in the top right.
7. Select **Start from Scratch** template and click **Create Skill**.

---

## Step 3: Set Invocation Name
1. In the left sidebar under *Interaction Model*, click **Invocation**.
2. **Skill Invocation Name**: Enter exactly:
   ```
   phone bridge
   ```
3. Click **Save Model**.

---

## Step 4: Import Interaction Model (One-Click)
Instead of typing every intent and utterance manually:
1. In the left sidebar, click **JSON Editor**.
2. Open the file in this repo:
   ```
   alexa-skill/interaction-model/en-IN.json (or alexa-interaction-model.json)
   ```
3. Copy its entire contents and paste into the editor.
4. Click **Save Model** at the top.
5. Click **Build Model**. Wait 1–2 minutes until you see *"Interaction Model built successfully"*.

---

## Step 5: Configure the Skill Endpoint

### Option A: AWS Lambda (Recommended)
1. In your AWS Management Console, create a new Lambda function (`Node.js 20.x`).
2. Add the **Alexa Skills Kit** trigger to the Lambda, pasting your **Skill ID** (from the Alexa Console).
3. Upload the zipped code from `alexa-skill/lambda/` (or copy `index.js`, `package.json`, and `services/`).
4. Set Lambda Environment Variables:
   - `BACKEND_URL`: Your public backend URL (e.g. `https://xyz.ngrok-free.app`).
   - `ALEXA_CLIENT_SECRET`: Matches `ALEXA_CLIENT_SECRET` in your backend `.env`.
5. Copy the Lambda **ARN** (e.g., `arn:aws:lambda:us-east-1:123456789012:function:PhoneBridge`).
6. In the Alexa Developer Console, click **Endpoint** in the left sidebar.
7. Select **AWS Lambda**, paste the ARN under **Default Region**, and click **Save Endpoints**.

### Option B: Alexa-Hosted (Node.js)
1. If you chose Alexa-hosted in Step 2:
2. Click the **Code** tab at the top of the Developer Console.
3. Paste the contents of `alexa-skill/lambda/index.js` into `index.js`.
4. Paste `alexa-skill/lambda/services/backendClient.js` into a new file `services/backendClient.js`.
5. Under Environment Variables in the Code tab, set:
   - `BACKEND_URL`: Your public ngrok / domain URL.
   - `ALEXA_CLIENT_SECRET`: Matches your backend `.env`.
6. Click **Save** and then **Deploy**.

### Option C: Direct HTTPS Endpoint
1. Select **HTTPS** under Endpoint in the Alexa Developer Console.
2. Enter your backend URL with `/api/alexa` (if using an Alexa express adapter).
3. Select SSL certificate type: *"My development endpoint is a sub-domain of a domain that has a wildcard certificate from a certificate authority"*.

---

## Step 6: Test in the Alexa Developer Console (Simulator)
1. Click the **Test** tab at the top.
2. Change **Skill testing is enabled in:** from *Off* to **Development**.
3. In the Alexa Simulator input bar, test the simulated flow:

### Test Case A: Query when no call exists
- Type or speak: `ask phone bridge who is calling`
- Expected speech: *"There is no incoming call right now."*

### Test Case B: Trigger a call & query caller
1. In your PhoneBridge Android app or test console, click **"Simulate Incoming Call"** (or use backend curl).
2. Type or speak: `ask phone bridge who is calling`
3. Expected speech: *"You have an incoming call from Rahul."*

### Test Case C: Answer the call
- Type or speak: `ask phone bridge to answer`
- Expected speech: *"Okay, I answered the call."*

### Test Case D: Reject the call
- Type or speak: `ask phone bridge to reject`
- Expected speech: *"Okay, I rejected the call."*

---

## Step 7: Test on Physical Echo Device
1. Ensure your physical Echo device is signed into the **exact same Amazon account** as your Developer Console.
2. In the Alexa mobile app (iOS / Android), navigate to **More > Skills & Games > Your Skills > Dev**.
3. Verify that **PhoneBridge** appears with status *In Development*.
4. Speak aloud near your Echo:
   > *"Alexa, ask Phone Bridge who is calling"*
   > *"Alexa, ask Phone Bridge to answer"*
   > *"Alexa, ask Phone Bridge to reject"*

---

## Step 8: Development vs. Public Certification

### For Personal Use (Default):
- You **do not need** to submit the skill for certification.
- As long as the skill status is in **Development**, it is indefinitely available on all Echo devices tied to your Amazon account!

### If Submitting for Public Publication:
- You must provide privacy policy and terms of use links.
- You must implement account linking (OAuth2) so other users can connect their own Android devices securely.
- Certification requires passing Amazon's automated voice and security reviews.
