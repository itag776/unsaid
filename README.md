# Unsaid.

> **Anonymous letters for your campus and the world.** > *Currently in Closed Testing on the Google Play Store.*

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple) ![Supabase](https://img.shields.io/badge/Backend-Supabase-green) ![Status](https://img.shields.io/badge/Status-Alpha_Testing-orange)

## 📱 About
**Unsaid** is an anonymous social network designed for university students. It allows users to share confessions ("letters") without revealing their identity, while ensuring community safety through verified email domains (e.g., `@university.edu`).

The app solves the problem of **"verified anonymity"**—proving you belong to a community without revealing who you are within it.

## 🚀 Key Features
* **Dual Feed Architecture:** Switch seamlessly between **Local Campus** feeds (filtered by university domain) and the **Global** feed.
* **True Anonymity:** Uses a "double-blind" identity system. We verify the email (Auth), but the content is stored with a hashed/randomized ID, decoupling the user from the post.
* **Safe Community:** Automated content moderation and reporting systems to comply with Google Play Safety Standards.
* **Smart Feed Logic:** Algorithmic content sorting ensures active and relevant posts appear first.

## 🛠️ Tech Stack
* **Language:** Kotlin (Android)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Backend:** Supabase (PostgreSQL)
* **Authentication:** OAuth (Google & Email) with custom token expiry logic for enterprise domains.
* **Database:** PostgreSQL with Row Level Security (RLS).


## 🔧 Setup & Installation
1.  **Clone the repo**
    ```bash
    git clone [https://github.com/itag776/unsaid.git](https://github.com/itag776/unsaid.git)
    ```
2.  **Open in Android Studio** (Ladybug or newer recommended).
3.  **Add `secrets.properties`**
    * Create a file named `secrets.properties` in the root directory.
    * Add your Supabase keys:
        ```properties
        SUPABASE_URL="your_url_here"
        SUPABASE_KEY="your_anon_key_here"
        ```
4.  **Run the app** on an Emulator or Physical Device.
