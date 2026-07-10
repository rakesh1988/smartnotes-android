package com.smartnotes.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun SmartNotesNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "note_list") {
        composable("note_list") {
            NoteListScreen(navController = navController)
        }
        composable(
            route = "note_editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) {
            NoteEditorScreen(navController = navController)
        }
        composable("note_editor/new") {
            NoteEditorScreen(navController = navController)
        }
    }
}
