import './initComponents.js';

export function formatSearchResult(results) {
  return results && results.jsonList || [];
}
