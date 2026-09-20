package com.tapshop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tapshop.shared.i18n.Language
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.SegmentedControl
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.settings.AppSettings
import com.tapshop.ui.theme.TapTheme
import com.tapshop.ui.theme.ThemeMode

/** Theme + language + about, shared by both apps. */
@Composable
fun SettingsContent(
    settings: AppSettings,
    modifier: Modifier = Modifier,
    extraAbout: List<Pair<String, String>> = emptyList(),
) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.appearance.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary, modifier = Modifier.padding(start = 4.dp))
            SurfaceCard(padding = PaddingValues(12.dp)) {
                SegmentedControl(
                    options = listOf(s.themeSystem, s.themeLight, s.themeDark),
                    selectedIndex = settings.themeMode.ordinal,
                    onSelected = { settings.updateThemeMode(ThemeMode.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.language.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary, modifier = Modifier.padding(start = 4.dp))
            SurfaceCard(padding = PaddingValues(0.dp)) {
                Language.entries.forEachIndexed { index, lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { settings.updateLanguage(lang) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(lang.nativeName, style = MaterialTheme.typography.bodyLarge, color = c.onSurface)
                            if (lang.nativeName != lang.englishName) {
                                Text(lang.englishName, style = MaterialTheme.typography.bodySmall, color = c.secondary)
                            }
                        }
                        if (lang == settings.language) {
                            Icon(Icons.Rounded.Check, null, tint = c.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (index != Language.entries.lastIndex) HairlineDivider(inset = 16.dp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.about.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary, modifier = Modifier.padding(start = 4.dp))
            SurfaceCard(padding = PaddingValues(0.dp)) {
                val rows = listOf(s.version to "0.1.0", s.deviceId to settings.uid, s.serverUrl to settings.apiBaseUrl) + extraAbout
                rows.forEachIndexed { index, (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                        Spacer(Modifier.weight(1f))
                        Text(value, style = MaterialTheme.typography.bodySmall, color = c.secondary, maxLines = 1)
                    }
                    if (index != rows.lastIndex) HairlineDivider(inset = 16.dp)
                }
            }
        }
    }
}
