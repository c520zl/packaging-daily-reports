import { reactive } from 'vue'

/**
 * 主题管理：浅色 / 深色 / 跟随系统
 * 通过给 <html> 加 theme-dark 类驱动 tokens.css 的暗色变量组
 */
export type ThemeMode = 'light' | 'dark' | 'auto'

const KEY = 'theme'

export const themeState = reactive({
  mode: (localStorage.getItem(KEY) as ThemeMode) || 'auto',
  isDark: false
})

function systemDark() {
  return matchMedia('(prefers-color-scheme: dark)').matches
}

function apply() {
  const dark = themeState.mode === 'dark' || (themeState.mode === 'auto' && systemDark())
  themeState.isDark = dark
  document.documentElement.classList.toggle('theme-dark', dark)
}

// 系统主题变化时，auto 模式实时跟随
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
  if (themeState.mode === 'auto') apply()
})

apply()

export function setTheme(mode: ThemeMode) {
  themeState.mode = mode
  localStorage.setItem(KEY, mode)
  apply()
}

export function toggleTheme() {
  setTheme(themeState.isDark ? 'light' : 'dark')
}
