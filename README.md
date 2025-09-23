# Java Snake Game

[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/technologies/javase-downloads.html)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A modern and feature-rich implementation of the classic Snake game, built with Java Swing. This version includes multiple difficulty levels, special food types, a persistent high score system, and a clean, intuitive user interface.

<p align="center">
  <!-- Replace this with a GIF or screenshot of your game -->
  <img width="876" height="913" alt="image" src="https://github.com/user-attachments/assets/63803a72-7329-4c0a-bdbf-e71f610b71ad" /><br/><br/>

  <img width="876" height="913" alt="image" src="https://github.com/user-attachments/assets/36e2ad5f-d0d9-4f5c-8859-4ac08b8abd41" />


</p>

## ✨ Features

- **Classic Gameplay:** Smooth, responsive controls for an authentic snake experience.
- **Multiple Difficulty Levels:** Choose between **Easy**, **Normal**, and **Hard** to match your skill. Each level adjusts the snake's speed and acceleration.
- **"Wrap Around" Mode:** An optional mode where the snake can pass through walls to appear on the opposite side.
- **Dynamic Food System:** A variety of food types to keep the gameplay interesting.
- **Persistent High Score:** Your best score is automatically saved to `highscore.txt` and displayed on the title screen.
- **Polished UI:** A clean interface with a title screen, in-game score display, and a game-over screen with restart options.
- **Pause/Resume:** Pause and resume the game at any time.

## 🍎 Dynamic Food System

The game features several types of food, each with a unique effect:

| Food Type     | Color   | Effect                                      |
|---------------|---------|---------------------------------------------|
| **Normal**    | Red     | Increases the snake's length by 1.          |
| **Bonus**     | Yellow  | Triples the snake's growth (adds 3 segments). |
| **Speed Boost** | Cyan    | Grants a temporary burst of super speed.    |
| **Shrink**    | Magenta | **Warning:** Makes your snake shorter!        |
| **Ghost**     | White   | Grants temporary immunity to wall collisions (in non-wrap mode). |

## 🎮 How to Play

### Objective
Control the snake to eat as much food as possible and grow its length. The game ends if the snake collides with itself or the walls (unless "Wrap Around" mode is enabled). Your score is the final length of your snake.

### Controls

| Key         | Action                  |
|-------------|-------------------------|
| `Arrow Keys`| Change the snake's direction. |
| `P`         | Pause or resume the game.     |

## 🚀 Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or later.

### Installation & Running

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/your-username/your-repo-name.git
    cd your-repo-name/src
    ```
    *(Or, download and extract the ZIP file and navigate to the `src` directory.)*

2.  **Compile the Java code:**
    Open a terminal or command prompt in the `src` directory and run:
    ```bash
    javac App.java
    ```

3.  **Run the game:**
    ```bash
    java App
    ```
    The game window will appear, starting with the title screen.

## 🔊 Enabling Sound (Optional)

The game includes code for sound effects that is disabled by default. To enable them:

1.  **Add Sound Files:** Place your `eat.wav` and `gameover.wav` sound files into the `src` directory.
2.  **Uncomment Code:** In `App.java`, find and uncomment the lines related to `loadSound` and `playSound`.
3.  **Recompile and Run:** Compile and run the game again as described above.
