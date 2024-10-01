package com.example.tingshingliu_chonghaochen_guesstheword

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells.Fixed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangmanGameApp()
        }
    }
}

class HangmanViewModel : ViewModel() {
    private var currentWord = getRandomWord()
    var word by mutableStateOf(currentWord.text)
    var guessedLetters by mutableStateOf(setOf<Char>())
    var incorrectGuesses by mutableStateOf(0)
    var hintState by mutableStateOf(0)
    var isGameOver by mutableStateOf(false)

    fun makeGuess(letter: Char) {
        if (letter in word) {
            guessedLetters = guessedLetters + letter
        } else {
            incorrectGuesses++
            guessedLetters = guessedLetters + letter
        }
        isGameOver = checkGameOver()
    }
    fun makeGuessAndGetMessage(letter: Char): String {
        makeGuess(letter)
        return if (letter in word) {
            "Correct! '$letter' is in the word."
        } else {
            "Incorrect. '$letter' is not in the word."
        }
    }
    private fun checkGameOver(): Boolean {
        return word.all { it in guessedLetters } || incorrectGuesses >= 6
    }

    fun useHint(): String {
        if (isGameOver || (incorrectGuesses >= 5 && hintState >= 1)) {
            return "Hint not available"
        }
        return when (hintState) {
            0 -> currentWord.hint
            1 -> disableHalfLetters()
            2 -> revealVowels()
            else -> "No more hints available"
        }.also {
            hintState++
            isGameOver = checkGameOver()
        }
    }

    private fun disableHalfLetters(): String {
        val remaining = ('A'..'Z').filterNot { it in guessedLetters || it in word }
        val toDisable = remaining.shuffled().take(remaining.size / 2)
        guessedLetters = guessedLetters + toDisable
        incorrectGuesses++
        return "Disabled half of the remaining letters"
    }

    private fun revealVowels(): String {
        val vowels = setOf('A', 'E', 'I', 'O', 'U')
        guessedLetters = guessedLetters + vowels
        incorrectGuesses++
        return "Revealed all vowels"
    }

    fun resetGame() {
        currentWord = getRandomWord()
        word = currentWord.text
        guessedLetters = setOf()
        incorrectGuesses = 0
        hintState = 0
        isGameOver = false
    }
}

data class Word(val text: String, val hint: String)

val words = listOf(
    Word("CAT", "A common household pet that meows"),
    Word("PIZZA", "A popular Italian dish with cheese and toppings"),
    Word("BEACH", "A sandy shore by the ocean"),
    Word("RAINBOW", "A colorful arc in the sky after rain"),
    Word("BICYCLE", "A two-wheeled vehicle powered by pedaling"),
    Word("BOOK", "A written or printed work of literature"),
    Word("MUSIC", "Vocal or instrumental sounds combined in a pleasing way"),
    Word("TREE", "A tall plant with a wooden trunk and branches"),
    Word("COMPUTER", "An electronic device for processing data"),
    Word("UMBRELLA", "A device used for protection against rain"),
    Word("ORIGAMI", "The Japanese art of paper folding"),
    Word("CAMERA", "A device used to capture images or videos"),
    Word("SUSHI", "A Japanese dish of prepared vinegared rice with various toppings"),
    Word("BALLOON", "An inflatable rubber")
)

@Composable
fun HangmanGameApp(viewModel: HangmanViewModel = viewModel()) {
    val orientation = LocalConfiguration.current.orientation

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                LetterSelectionPanel(viewModel, Modifier.weight(1f))
                HintPanel(viewModel)
            }
            Column(modifier = Modifier.weight(1f)) {
                MainGamePlayScreen(viewModel)
                NewGameButton(viewModel)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            MainGamePlayScreen(viewModel)
            LetterSelectionPanel(viewModel, Modifier.weight(1f))
            NewGameButton(viewModel)
        }
    }
}

@Composable
fun LetterSelectionPanel(viewModel: HangmanViewModel, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        LazyVerticalGrid(columns = Fixed(6), modifier = Modifier.fillMaxSize()) {
            items(('A'..'Z').toList()) { letter ->
                Button(
                    onClick = {
                        val message = viewModel.makeGuessAndGetMessage(letter)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    enabled = !viewModel.isGameOver && letter !in viewModel.guessedLetters,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(letter.toString())
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)

        )
    }
}
@Composable
fun HintPanel(viewModel: HangmanViewModel) {
    val hintMessage = remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column {
        Text(
            text = hintMessage.value,
            fontSize = 60.sp,
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = {
                val message = viewModel.useHint()
                if (message == "Hint not available") {
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                } else {
                    hintMessage.value = message
                }
            },
            enabled = !viewModel.isGameOver && viewModel.hintState < 3,
            modifier = Modifier.padding(16.dp)
                .size(100.dp)
        ) {
            Text("Hint")
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun MainGamePlayScreen(viewModel: HangmanViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = viewModel.word.map { if (it in viewModel.guessedLetters) it else '_' }.joinToString(" "),
            fontSize = 50.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HangmanFigure(viewModel.incorrectGuesses)

        if (viewModel.isGameOver) {
            Text(
                text = if (viewModel.word.all { it in viewModel.guessedLetters }) "You won!" else "You lost!",
                fontSize = 30.sp
            )
        }
    }
}

@Composable
fun HangmanFigure(incorrectGuesses: Int) {
    val orientation = LocalConfiguration.current.orientation
    var hangmansize by remember { mutableStateOf(0) }
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) { hangmansize = 550 } else { hangmansize = 300 }

    Canvas(modifier = Modifier.size(hangmansize.dp)) {
        // Base
        drawLine(Color.Black, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 5f)
        // Pole
        drawLine(Color.Black, Offset(size.width / 2, size.height), Offset(size.width / 2, 0f), strokeWidth = 5f)
        // Top Beam
        drawLine(Color.Black, Offset(size.width / 2, 0f), Offset(size.width * 0.75f, 0f), strokeWidth = 5f)
        // Rope
        drawLine(Color.Black, Offset(size.width * 0.75f, 0f), Offset(size.width * 0.75f, size.height * 0.2f), strokeWidth = 5f)
        // Head
        if (incorrectGuesses > 0) {
            drawCircle(
                Color.Black,
                radius = size.width * 0.05f,
                center = Offset(size.width * 0.75f, size.height * 0.25f)
            )
        }
        // Body
        if (incorrectGuesses > 1) {
            drawLine(
                Color.Black,
                Offset(size.width * 0.75f, size.height * 0.3f),
                Offset(size.width * 0.75f, size.height * 0.5f),
                strokeWidth = 5f
            )
        }
        // Arms
        if (incorrectGuesses > 2) {
            drawLine(
                Color.Black,
                Offset(size.width * 0.75f, size.height * 0.35f),
                Offset(size.width * 0.7f, size.height * 0.4f),
                strokeWidth = 5f
            )
        }
        if (incorrectGuesses > 3) {
            drawLine(
                Color.Black,
                Offset(size.width * 0.75f, size.height * 0.35f),
                Offset(size.width * 0.8f, size.height * 0.4f),
                strokeWidth = 5f
            )
        }
        // Legs
        if (incorrectGuesses > 4) {
            drawLine(
                Color.Black,
                Offset(size.width * 0.75f, size.height * 0.5f),
                Offset(size.width * 0.7f, size.height * 0.6f),
                strokeWidth = 5f
            )
        }
        if (incorrectGuesses > 5) {
            drawLine(
                Color.Black,
                Offset(size.width * 0.75f, size.height * 0.5f),
                Offset(size.width * 0.8f, size.height * 0.6f),
                strokeWidth = 5f
            )
        }
    }
}

@Composable
fun NewGameButton(viewModel: HangmanViewModel) {
    Button(
        onClick = { viewModel.resetGame() },
        modifier = Modifier.padding(16.dp)
    ) {
        Text("New Game")
    }
}

fun getRandomWord(): Word {
    return words[Random.nextInt(words.size)]
}

@Preview(showBackground = true)
@Composable
fun PreviewHangman() {
    HangmanGameApp()
}