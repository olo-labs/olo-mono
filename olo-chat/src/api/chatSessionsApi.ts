/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { CHAT_API, withAuth } from './chatApiClient'
import type { ChatMessageDto, CreateSessionResponse, SendMessageResponse, SessionSummaryDto } from './chatTypes'
const API = `${CHAT_API}/v1/conversations`
async function checked(res: Response): Promise<Response> {
 if (!res.ok) throw new Error(`Conversation request failed: ${res.status}`)
 return res
}
const writeHeaders = () => withAuth({'Content-Type':'application/json','Idempotency-Key':crypto.randomUUID()})
export async function listSessions(_tenant: string, _options?: {queue?:string;pipeline?:string}): Promise<SessionSummaryDto[]> {
 return (await checked(await fetch(API,{headers:withAuth()}))).json()
}
export async function deleteAllSessions(tenant:string, options?:{queue?:string;pipeline?:string}):Promise<void> {
 for(const session of await listSessions(tenant,options)) await deleteSession(session.sessionId)
}
export async function deleteSession(id:string):Promise<void> {
 await checked(await fetch(`${API}/${encodeURIComponent(id)}`,{method:'DELETE',headers:writeHeaders()}))
}
export async function createSession(_tenant:string,_options?:{taskQueue?:string;queueName?:string;pipelineId?:string;overrides?:Record<string,unknown>}):Promise<CreateSessionResponse> {
 const sessionId=crypto.randomUUID()
 await checked(await fetch(`${API}/${sessionId}`,{method:'POST',headers:writeHeaders()}))
 return {sessionId}
}
export async function sendMessage(id:string, content:string, options?:{taskQueue?:string;capabilitySource?:string;pipelineId?:string}):Promise<SendMessageResponse> {
 return (await checked(await fetch(`${API}/${encodeURIComponent(id)}/turns`,{method:'POST',headers:writeHeaders(),body:JSON.stringify({message:content,kbId:options?.capabilitySource || undefined,pipelineId:options?.pipelineId || undefined})}))).json()
}
export async function listMessages(id:string):Promise<ChatMessageDto[]> {
 const data=await (await checked(await fetch(`${API}/${encodeURIComponent(id)}/messages`,{headers:withAuth()}))).json()
 return (data.messages ?? []).filter((m:{role:string})=>m.role==='user'||m.role==='assistant').map((m:{message_id:string;turn_id:string;role:string;content:string;sequence:number})=>({messageId:m.message_id,sessionId:id,role:m.role,content:m.content,runId:m.turn_id,createdAt:m.sequence}))
}
