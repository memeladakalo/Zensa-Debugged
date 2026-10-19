package com.zensa.app.ui.navigation

/**
 * Routes
 *
 * Every navigation route in Zensa lives here as a constant.
 * Nothing in this file knows about Compose or Navigation —
 * it only defines strings, so it can be referenced from anywhere.
 */
object Routes {

    // ---------------- Auth ----------------
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // ---------------- Main sections ----------------
    const val DASHBOARD = "dashboard"
    const val MODULES = "modules"
    const val ASSIGNMENTS = "assignments"
    const val NOTES = "notes"
    const val TIMER = "timer"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"

    // ---------------- Assignments (parameterised) ----------------
    const val ASSIGNMENT_FORM = "assignment_form"
    const val ASSIGNMENT_FORM_EDIT = "assignment_form/{localId}"
    const val ASSIGNMENT_DETAIL = "assignment_detail/{localId}"

    fun assignmentFormEdit(localId: String): String = "assignment_form/$localId"
    fun assignmentDetail(localId: String): String = "assignment_detail/$localId"

    // ---------------- Notes (parameterised) ----------------
    const val NOTE_FORM = "note_form"
    const val NOTE_FORM_EDIT = "note_form/{localId}"

    fun noteFormEdit(localId: String): String = "note_form/$localId"

    // ---------------- Arguments ----------------
    const val ARG_ASSIGNMENT_LOCAL_ID = "localId"
}
