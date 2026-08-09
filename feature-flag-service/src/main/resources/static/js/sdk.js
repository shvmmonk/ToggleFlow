/**
 * Feature Flag Service - Lightweight Client SDK
 * 
 * Usage Example:
 *   const ff = new FeatureFlagSDK({
 *     baseUrl: 'http://localhost:8080',
 *     projectApiKey: 'ff_live_...'
 *   });
 * 
 *   const isEnabled = await ff.isFeatureEnabled('dark_mode', 'user_123');
 */
class FeatureFlagSDK {
    constructor(config = {}) {
        this.baseUrl = (config.baseUrl || window.location.origin).replace(/\/$/, '');
        this.projectApiKey = config.projectApiKey || '';
        this.cache = new Map();
        this.ttlMs = config.ttlMs || 30000; // 30 seconds local client memory cache
    }

    /**
     * Evaluates a feature flag for a specific user.
     * @param {string} flagKey - The key of the feature flag (e.g. 'dark_mode')
     * @param {string} userId - Unique identifier for the user (e.g. 'user_123' or 'anon-session')
     * @param {boolean} defaultValue - Fallback value if request fails (default: false)
     * @returns {Promise<boolean>}
     */
    async isFeatureEnabled(flagKey, userId, defaultValue = false) {
        if (!this.projectApiKey) {
            console.warn('[FeatureFlagSDK] Missing projectApiKey');
            return defaultValue;
        }

        const cacheKey = `${flagKey}:${userId}`;
        const cached = this.cache.get(cacheKey);

        if (cached && (Date.now() - cached.timestamp < this.ttlMs)) {
            return cached.enabled;
        }

        try {
            const url = `${this.baseUrl}/api/evaluate/${encodeURIComponent(flagKey)}?projectApiKey=${encodeURIComponent(this.projectApiKey)}&userId=${encodeURIComponent(userId)}`;
            const response = await fetch(url);

            if (!response.ok) {
                console.warn(`[FeatureFlagSDK] Server returned status ${response.status}`);
                return defaultValue;
            }

            const data = await response.json();
            const result = Boolean(data.enabled);

            this.cache.set(cacheKey, {
                enabled: result,
                timestamp: Date.now()
            });

            return result;
        } catch (error) {
            console.error('[FeatureFlagSDK] Evaluation error:', error);
            return defaultValue;
        }
    }

    /**
     * Clears local client cache
     */
    clearCache() {
        this.cache.clear();
    }
}

// Global convenience window method
window.FeatureFlagSDK = FeatureFlagSDK;
