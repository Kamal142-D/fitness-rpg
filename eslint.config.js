// Flat ESLint config. Extends Expo's shared config; Prettier runs separately
// (see the `format` script) so it does not fight ESLint over formatting.
const expoConfig = require('eslint-config-expo/flat');

module.exports = [
  ...expoConfig,
  {
    ignores: ['dist/*', 'node_modules/*', '.expo/*', 'expo-env.d.ts'],
  },
];
