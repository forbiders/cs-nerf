package com.lagradost.cloudstream3.ui.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.BasePreferenceFragmentCompat
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.getPref
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setPaddingBottom
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setToolBarScrollFlags
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setUpToolbar
import com.lagradost.cloudstream3.utils.AppContextUtils.getApiDubstatusSettings
import com.lagradost.cloudstream3.utils.AppContextUtils.getApiProviderLangSettings
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showDialog
import com.lagradost.cloudstream3.utils.SingleSelectionHelper.showMultiDialog
import com.lagradost.cloudstream3.utils.SourceLanguageHelper
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromTagToLanguageName
import com.lagradost.cloudstream3.utils.SubtitleHelper.getNameNextToFlagEmoji
import com.lagradost.cloudstream3.utils.UIHelper.hideKeyboard

class SettingsProviders : BasePreferenceFragmentCompat() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpToolbar(R.string.category_providers)
        setPaddingBottom()
        setToolBarScrollFlags()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        hideKeyboard()
        setPreferencesFromResource(R.xml.settings_providers, rootKey)
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        getPref(R.string.display_sub_key)?.setOnPreferenceClickListener {
            activity?.getApiDubstatusSettings()?.let { current ->
                val dublist = DubStatus.entries
                val names = dublist.map { it.name }

                val currentList = ArrayList<Int>()
                for (i in current) {
                    currentList.add(dublist.indexOf(i))
                }

                activity?.showMultiDialog(
                    names,
                    currentList,
                    getString(R.string.display_subbed_dubbed_settings),
                    {}
                ) { selectedList ->
                    APIRepository.dubStatusActive = selectedList.map { dublist[it] }.toHashSet()
                    settingsManager.edit {
                        putStringSet(
                            getString(R.string.display_sub_key),
                            selectedList.map { names[it] }.toMutableSet()
                        )
                    }
                }
            }

            return@setOnPreferenceClickListener true
        }

        getPref(R.string.test_providers_key)?.setOnPreferenceClickListener {
            // Somehow animations do not work without this.
            val options = NavOptions.Builder()
                .setEnterAnim(R.anim.enter_anim)
                .setExitAnim(R.anim.exit_anim)
                .setPopEnterAnim(R.anim.pop_enter)
                .setPopExitAnim(R.anim.pop_exit)
                .build()

            this@SettingsProviders.findNavController()
                .navigate(R.id.navigation_test_providers, null, options)
            true
        }

        getPref(R.string.prefer_media_type_key)?.setOnPreferenceClickListener {
            // Torrent/P2P removed in this build: hide TvType.Torrent from choices
            // but preserve any stored ordinal so old prefs do not corrupt.
            val visibleTypes = enumValues<TvType>().sorted().filter { it != TvType.Torrent }
            val names = visibleTypes.map { it.name }
            val default =
                enumValues<TvType>().sorted().filter { it != TvType.NSFW }.map { it.ordinal }
            val defaultSet = default.map { it.toString() }.toSet()
            val currentOrdinals = try {
                settingsManager.getStringSet(getString(R.string.prefer_media_type_key), defaultSet)
                    ?.mapNotNull { it.toIntOrNull() }
            } catch (e: Throwable) {
                null
            } ?: default
            val currentList = currentOrdinals.mapNotNull { ordinal ->
                visibleTypes.indexOfFirst { it.ordinal == ordinal }.takeIf { it >= 0 }
            }

            activity?.showMultiDialog(
                names,
                currentList,
                getString(R.string.preferred_media_settings),
                {}
            ) { selectedList ->
                settingsManager.edit {
                    putStringSet(
                        getString(R.string.prefer_media_type_key),
                        selectedList.map { visibleTypes[it].ordinal.toString() }.toMutableSet()
                    )
                }
                DataStoreHelper.currentHomePage = null
                //(context ?: CloudStreamApp.context)?.let { ctx -> app.initClient(ctx) }
            }

            return@setOnPreferenceClickListener true
        }

        getPref(R.string.preferred_source_language_key)?.setOnPreferenceClickListener {
            val tags = listOf("") + SourceLanguageHelper.COMMON_SOURCE_TAGS
            val displayNames = tags.map { tag ->
                if (tag.isBlank()) getString(R.string.none)
                else fromTagToLanguageName(tag) ?: tag
            }
            val currentTag = try {
                settingsManager.getString(getString(R.string.preferred_source_language_key), "")
            } catch (_: Throwable) {
                ""
            } ?: ""
            val selectedIndex = tags.indexOf(currentTag).takeIf { it >= 0 } ?: 0
            activity?.showDialog(
                displayNames,
                selectedIndex,
                getString(R.string.preferred_source_language_settings),
                showApply = false,
                dismissCallback = {}
            ) { selected ->
                settingsManager.edit {
                    putString(getString(R.string.preferred_source_language_key), tags[selected])
                }
            }
            return@setOnPreferenceClickListener true
        }

        getPref(R.string.
        provider_lang_key)?.setOnPreferenceClickListener {
            activity?.getApiProviderLangSettings()?.let { currentLangTags ->
                val languagesTagName = APIHolder.apis.withLock {
                    listOf(Pair(AllLanguagesName, getString(R.string.all_languages_preference))) +
                        APIHolder.apis.map { Pair(it.lang, getNameNextToFlagEmoji(it.lang) ?: it.lang) }
                            .toSet().sortedBy { it.second.substringAfter("\u00a0").lowercase() }
                }

                val currentIndexList = currentLangTags.map { langTag ->
                    languagesTagName.indexOfFirst { lang -> lang.first == langTag }
                }

                activity?.showMultiDialog(
                    languagesTagName.map { it.second },
                    currentIndexList,
                    getString(R.string.provider_lang_settings),
                    {}
                ) { selectedList ->
                    settingsManager.edit {
                        putStringSet(
                            getString(R.string.provider_lang_key),
                            selectedList.map { languagesTagName[it].first }.toSet()
                        )
                    }
                    // APIRepository.providersActive = it.context.getApiSettings()
                }
            }

            return@setOnPreferenceClickListener true
        }
    }
}
