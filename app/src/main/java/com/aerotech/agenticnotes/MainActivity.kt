@file:Suppress("UNCHECKED_CAST")

package com.aerotech.agenticnotes

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import kotlin.math.max

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
                    composable("binScreen") {
                        BinScreen(navController = navController, viewModel = noteViewModel)
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

private val mainBackgroundColor = Color(37, 150, 190)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNotesScreen(navController: NavController, viewModel: NoteViewModel) {
    val notes by viewModel.notes.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Notes", "Calendar")

    // State for multi-select and menu
    val selectedIds by viewModel.selectedNoteIds.collectAsState()
    val isMultiSelectActive by viewModel.isMultiSelectActive.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Clear selection when leaving the screen or multi-select is deactivated
    // Also clears when switching tabs
    LaunchedEffect(isMultiSelectActive, selectedTab) {
        if (!isMultiSelectActive) {
            viewModel.clearSelection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Show selected count or tabs
                    if (isMultiSelectActive) {
                        Text(
                            "${selectedIds.size} selected",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            indicator = { /* We can hide the default indicator */ },
                            divider = { /* And the divider */ }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color.LightGray.copy(alpha = 0.7f)
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    // 3-dot menu
                    if (!isMultiSelectActive) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bin") },
                                onClick = {
                                    showMenu = false
                                    showPasswordDialog = true // Show password dialog first
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings (soon)") },
                                onClick = { showMenu = false /* TODO */ }
                            )
                        }
                    }
                },
                navigationIcon = {
                    // Show back arrow to exit multi-select mode
                    if (isMultiSelectActive) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Clear Selection", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            // Only show FAB if not in multi-select mode and on the Notes tab
            if (!isMultiSelectActive && selectedTab == 0) {
                FloatingActionButton(
                    onClick = { navController.navigate("noteEditScreen") },
                    containerColor = Color(0xFFF5F5F5),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Note", tint = mainBackgroundColor)
                }
            }
        },
        bottomBar = {
            // Bottom bar for delete action
            AnimatedVisibility(
                visible = isMultiSelectActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomAppBar(
                    containerColor = mainBackgroundColor.copy(alpha = 0.95f),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.moveSelectedNotesToBin() }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.White)
                            Text("Delete", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        },
        containerColor = mainBackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            if (selectedTab == 0) {
                SearchBar(viewModel = viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                NotesGrid(
                    notes = notes,
                    selectedIds = selectedIds,
                    isMultiSelectActive = isMultiSelectActive,
                    viewModel = viewModel,
                    navController = navController
                )
            } else {
                CalendarPlaceholder()
            }
        }

        if (showPasswordDialog) {
            PasswordEntryDialog(
                onDismiss = { showPasswordDialog = false },
                onSuccess = {
                    showPasswordDialog = false
                    navController.navigate("binScreen")
                }
            )
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
            .background(Color(0xFF999090), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        textStyle = TextStyle(color = Color.White, fontSize = 22.sp),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text("Search notes...", color = Color.White.copy(alpha = 0.7f), fontSize = 22.sp)
                }
                innerTextField()
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesGrid(
    notes: List<Note>,
    selectedIds: Set<UUID>,
    isMultiSelectActive: Boolean,
    viewModel: NoteViewModel,
    navController: NavController
) {
    if (notes.isEmpty() && viewModel.searchQuery.value.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No notes yet. Tap '+' to add one!", color = Color.White.copy(alpha = 0.8f), fontSize = 22.sp, textAlign = TextAlign.Center)
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
            val isSelected = note.id in selectedIds
            Box {
                Column(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(Color(0xFFD9D9D9), RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = {
                                if (isMultiSelectActive) {
                                    viewModel.toggleNoteSelection(note.id)
                                } else {
                                    navController.navigate("noteEditScreen/${note.id}")
                                }
                            },
                            onLongClick = {
                                viewModel.toggleNoteSelection(note.id)
                            }
                        )
                        .padding(12.dp)
                ) {
                    Text(text = note.title, color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = note.content, color = Color.DarkGray, fontSize = 20.sp, maxLines = 4)
                }

                if (isMultiSelectActive) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Select",
                        tint = if (isSelected) mainBackgroundColor else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinScreen(navController: NavController, viewModel: NoteViewModel) {
    val binnedNotes by viewModel.binnedNotes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bin", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = mainBackgroundColor
    ) { innerPadding ->
        if (binnedNotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("The bin is empty.", color = Color.White.copy(alpha = 0.8f), fontSize = 22.sp)
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(innerPadding)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(binnedNotes, key = { it.id }) { note ->
                Box(modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color(0xFFD9D9D9), RoundedCornerShape(10.dp))) {
                    Column(Modifier.padding(12.dp)) {
                        Text(note.title, fontWeight = FontWeight.Bold, maxLines = 2)
                        Spacer(Modifier.height(4.dp))
                        Text(note.content, maxLines = 3, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        val daysLeft = max(0, 30 - ((System.currentTimeMillis() - (note.binnedTimestamp ?: 0)) / (1000 * 60 * 60 * 24)))
                        Text("Deletes in $daysLeft days", fontSize = 12.sp, color = Color.Red.copy(alpha = 0.8f))
                    }
                    IconButton(
                        onClick = { viewModel.restoreNote(note.id) },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(Icons.Default.Restore, "Restore")
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordEntryDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val correctPassword = "wakaranai"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enter Password", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = "" },
                    label = { Text("Password") },
                    isError = error.isNotEmpty(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (password == correctPassword) {
                            onSuccess()
                        } else {
                            error = "Incorrect password"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter")
                }
            }
        }
    }
}

@Composable
fun CalendarPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Calendar feature is under construction.",
            fontSize = 24.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = { /* TODO: Implement AI feature on the current note */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Text("Agentic", color = Color.Black, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = mainBackgroundColor
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
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (titleText.isEmpty()) {
                                Text("Title", color = Color.White.copy(alpha = 0.7f), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            }
                            innerTextField()
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                BasicTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 22.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteContent.isEmpty()) {
                                Text("Start writing here...", color = Color.White.copy(alpha = 0.7f), fontSize = 22.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}
