/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { afterEach, describe, expect, it, vi } from 'vitest'
vi.mock('./chatApiClient',()=>({CHAT_API:'/api',withAuth:(headers={})=>headers}))
import {createSession,sendMessage,listMessages} from './chatSessionsApi'
afterEach(()=>vi.unstubAllGlobals())
describe('governed chat',()=>{
 it('creates an opaque conversation with an idempotency key',async()=>{
  const fetcher=vi.fn().mockResolvedValue({ok:true});vi.stubGlobal('fetch',fetcher)
  const {sessionId}=await createSession('caller-controlled-tenant')
  expect(fetcher).toHaveBeenCalledWith(`/api/v1/conversations/${sessionId}`,expect.objectContaining({method:'POST',headers:expect.objectContaining({'Idempotency-Key':expect.any(String)})}))
 })
 it('sends message and KB reference without tenant or actor authority',async()=>{
  const fetcher=vi.fn().mockResolvedValue({ok:true,json:async()=>({runId:'turn',messageId:'msg'})});vi.stubGlobal('fetch',fetcher)
  await sendMessage('conversation','hello',{capabilitySource:'kb',taskQueue:'untrusted'})
  expect(JSON.parse(fetcher.mock.calls[0][1].body)).toEqual({message:'hello',kbId:'kb'})
 })
 it('maps durable history and filters internal working roles',async()=>{
  vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:true,json:async()=>({messages:[{message_id:'m',turn_id:'r',role:'assistant',content:'answer',sequence:2},{role:'pending',content:'hidden'}]})}))
  expect(await listMessages('c')).toEqual([{messageId:'m',sessionId:'c',role:'assistant',content:'answer',runId:'r',createdAt:2}])
 })
 it('surfaces authorization failure instead of returning an empty history',async()=>{
  vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:false,status:403}))
  await expect(listMessages('c')).rejects.toThrow('403')
 })
})
