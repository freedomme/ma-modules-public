/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Luis Güette
 */

MangoApiSettingsFactory.$inject = ['maRestResource', '$http'];
function MangoApiSettingsFactory(RestResource, $http) {
    
    const baseUrl = '/rest/v2/server/cors-settings';
    const xidPrefix = 'APISET_';

    class MangoApiSettingsResource extends RestResource {

        static get baseUrl() {
            return baseUrl;
        }

        static get xidPrefix() {
            return xidPrefix;
        }

        static getCorsSettings() {
            let url, method;
            url = baseUrl;
            method = 'GET';

            return $http({
                url,
                method
            }).then(response => {
                return response.data;
            }); 
        }
    
    }
    
    return MangoApiSettingsResource;
}

export default MangoApiSettingsFactory;