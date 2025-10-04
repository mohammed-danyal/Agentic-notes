@file:Suppress("UNCHECKED_CAST")

package com.aerotech.agenticnotes

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aerotech.agenticnotes.ui.theme.AgenticNotesTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private val noteViewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgenticNotesTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "mainScreen") {
                    composable("mainScreen") {
                        MainNotesScreen(navController = navController, viewModel = noteViewModel)
                    }
                    composable("noteEditScreen") {
                        NoteEditScreen(
                            navController = navController,
                            viewModel = noteViewModel,
                            noteId = null
                        )
                    }
                    composable(
                        "noteEditScreen/{noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getString("noteId")
                        NoteEditScreen(
                            navController = navController,
                            viewModel = noteViewModel,
                            noteId = noteId?.let { UUID.fromString(it) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNotesScreen(navController: NavController, viewModel: NoteViewModel) {
    val notes by viewModel.notes.collectAsState()

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("noteEditScreen") },
                containerColor = Color(0xFF8E8484),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Note", tint = Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SearchBar(viewModel = viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            NotesGrid(notes = notes, navController = navController)
        }
    }
}

@Composable
fun SearchBar(viewModel: NoteViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()

    BasicTextField(
        value = searchQuery,
        onValueChange = { viewModel.onSearchQueryChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
        singleLine = true,
        decorationBox = {
            innerTextField -> Box(contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text("Search notes...", color = Color.Gray)
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun NotesGrid(notes: List<Note>, navController: NavController) {
    if (notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No notes yet. Tap '+' to add one!", color = Color.Gray)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            Column(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .clickable { navController.navigate("noteEditScreen/${note.id}") }
                    .padding(12.dp)
            ) {
                Text(text = note.title, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = note.content, color = Color.DarkGray, fontSize = 14.sp, maxLines = 4)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(navController: NavController, viewModel: NoteViewModel, noteId: UUID?) {
    var titleText by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    val isNoteLoaded = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = noteId) {
        if (noteId != null) {
            val existingNote = viewModel.getNoteById(noteId)
            if (existingNote != null) {
                withFrameNanos {
                    titleText = existingNote.title
                    noteContent = existingNote.content
                    isNoteLoaded.value = true
                }
            }
        } else {
            isNoteLoaded.value = true
        }
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            // This block now only runs ONCE when you leave the screen.
            // It reads the latest state at that moment and tells the ViewModel to save.
            viewModel.saveNoteOnExit(
                noteId = noteId,
                title = titleText,
                content = noteContent
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // ADD THE ACTIONS BLOCK HERE
                actions = {
                    Button(
                        onClick = { /* TODO: Implement AI feature on the current note */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                    ) {
                        Text("Agentic", color = Color.Black)
                    }
                    // Add a little space before the edge
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        if (!isNoteLoaded.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .imePadding()
            ) {
                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (titleText.isEmpty()) {
                                Text("Title", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            innerTextField()
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )
                BasicTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(color = Color.DarkGray, fontSize = 16.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteContent.isEmpty()) {
                                Text("Start writing here...", color = Color.Gray, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

// --- PREVIEWS ---

// Dummy ViewModel factory for previews
class DummyViewModelFactory : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return NoteViewModel(Application()) as T
    }
}

@Preview(showBackground = true)
@Composable
fun MainNotesScreenPreview() {
    AgenticNotesTheme {
        MainNotesScreen(
            navController = rememberNavController(),
            viewModel = viewModel(factory = DummyViewModelFactory())
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoteEditScreenPreview() {
    AgenticNotesTheme {
        NoteEditScreen(
            navController = rememberNavController(),
            viewModel = viewModel(factory = DummyViewModelFactory()),
            noteId = null
        )
    }
}
