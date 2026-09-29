package com.leonardo.apptreino.ui.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.AppMode
import com.leonardo.apptreino.databinding.ActivityMainBinding
import com.leonardo.apptreino.ui.profile.ProfileFragment
import com.leonardo.apptreino.ui.students.StudentsFragment
import com.leonardo.apptreino.ui.today.TodayFragment
import com.leonardo.apptreino.ui.week.WeekFragment
import kotlinx.coroutines.launch

/**
 * Tela principal (1ª tela). Hospeda as abas como Fragments, trocados pela Bottom Navigation:
 * - modo Aluno: Hoje, Semana e Perfil;
 * - modo Coach: Alunos e Perfil.
 *
 * Estratégia de troca: cada Fragment é adicionado uma única vez e depois apenas
 * mostrado/escondido (show/hide). Assim a posição de rolagem de cada aba é preservada.
 * O espaço das barras do sistema (edge-to-edge) é tratado por cada aba.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Mesma instância usada pelos Fragments (activityViewModels). */
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // A splash precisa ser instalada ANTES do super.onCreate().
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        listenForTabRequests()

        // Primeira abertura: mostra a aba "Hoje". Após rotação/recriação, o FragmentManager
        // e a Bottom Navigation já restauram sozinhos o estado anterior.
        if (savedInstanceState == null) {
            showTab(R.id.nav_today)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mode.collect(::applyMode)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        // Tocar de novo na aba atual rola o conteúdo para o topo (padrão de apps populares).
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            (supportFragmentManager.findFragmentByTag(tagFor(item.itemId)) as? ScrollableToTop)
                ?.scrollToTop()
        }
    }

    /** Mostra só as abas do modo atual. Se a aba aberta sumiu, vai para a primeira do modo. */
    private fun applyMode(mode: AppMode) {
        val isCoach = mode == AppMode.COACH
        val menu = binding.bottomNavigation.menu
        menu.findItem(R.id.nav_today).isVisible = !isCoach
        menu.findItem(R.id.nav_week).isVisible = !isCoach
        menu.findItem(R.id.nav_students).isVisible = isCoach

        val selected = menu.findItem(binding.bottomNavigation.selectedItemId)
        if (selected == null || !selected.isVisible) {
            binding.bottomNavigation.selectedItemId = if (isCoach) R.id.nav_students else R.id.nav_today
        }
    }

    /**
     * Os Fragments pedem troca de aba pela Fragment Result API (ex.: tocar num dia da
     * aba "Semana" abre a aba "Hoje"). Assim eles não dependem diretamente da Activity.
     */
    private fun listenForTabRequests() {
        supportFragmentManager.setFragmentResultListener(REQUEST_OPEN_TAB, this) { _, result ->
            binding.bottomNavigation.selectedItemId = result.getInt(KEY_TAB_ID)
        }
    }

    private fun showTab(@IdRes tabId: Int) {
        val fm = supportFragmentManager
        val targetTag = tagFor(tabId)

        fm.commit {
            setReorderingAllowed(true)
            TAB_IDS.map(::tagFor)
                .filter { it != targetTag }
                .mapNotNull(fm::findFragmentByTag)
                .forEach { hide(it) }

            val existing = fm.findFragmentByTag(targetTag)
            if (existing == null) {
                add(R.id.fragment_container, createFragment(tabId), targetTag)
            } else {
                show(existing)
            }
        }
    }

    private fun createFragment(@IdRes tabId: Int): Fragment = when (tabId) {
        R.id.nav_week -> WeekFragment()
        R.id.nav_students -> StudentsFragment()
        R.id.nav_profile -> ProfileFragment()
        else -> TodayFragment()
    }

    private fun tagFor(@IdRes tabId: Int): String = "tab_$tabId"

    /** Implementado pelas abas que sabem rolar de volta ao topo. */
    interface ScrollableToTop {
        fun scrollToTop()
    }

    companion object {
        private val TAB_IDS = listOf(R.id.nav_today, R.id.nav_week, R.id.nav_students, R.id.nav_profile)

        const val REQUEST_OPEN_TAB = "request_open_tab"
        const val KEY_TAB_ID = "tab_id"

        /** Pede à MainActivity que selecione a aba [tabId]. */
        fun requestTab(fragmentManager: FragmentManager, @IdRes tabId: Int) {
            val result = Bundle().apply { putInt(KEY_TAB_ID, tabId) }
            fragmentManager.setFragmentResult(REQUEST_OPEN_TAB, result)
        }
    }
}
