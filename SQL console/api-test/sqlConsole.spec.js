/**
 * Copyright 2018 Infinite Automation Systems Inc.
 * http://infiniteautomation.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const {createClient, login} = require('@infinite-automation/mango-module-tools/test-helper/testHelper');
const client = createClient();

describe('SQL Console ', () => {
    before('Login', function() { return login.call(this, client); });
    
    it('List tables', function(){
        return client.restRequest({
            path: '/rest/v2/sql-console/list-tables',
            method: 'GET',
        }).then(response => {
            assert.strictEqual(response.data.headers.length, 1);
            assert.isAbove(response.data.data.length, 0);
        });
      });

    it('Query', function(){
        return client.restRequest({
            path: '/rest/v2/sql-console',
            method: 'GET',
            params: {
                query: 'SELECT * FROM USERS;'
            }
        }).then(response => {
            assert.isAbove(response.data.headers.length, 17);
            assert.isAbove(response.data.data.length, 0);
        });
      });

    it('Update', function(){
        return client.restRequest({
            path: '/rest/v2/sql-console',
            method: 'POST',
            headers: {
                'Content-Type': 'application/sql'
            },
            data: "UPDATE USERS set name = 'testing';"
        }).then(response => {
            assert.isAbove(response.data, 0);
        });
      });
    
    
});
