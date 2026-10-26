/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { useState } from 'react'
import { knowledgeOperation } from '../api/governedKnowledgeApi'
export function GovernedKnowledgeView() {
 const [kb,setKb]=useState<string>(()=>crypto.randomUUID())
 const [doc,setDoc]=useState<string>(()=>crypto.randomUUID())
 const [version,setVersion]=useState(0)
 const [content,setContent]=useState('')
 const [query,setQuery]=useState('')
 const [member,setMember]=useState('')
 const [result,setResult]=useState('')
 const [busy,setBusy]=useState(false)
 async function perform(operation:'ingest'|'update'|'delete'|'query'|'share') {
  setBusy(true);setResult('')
  try {
   const body=operation==='query'?{query}:operation==='share'?{docId:doc,member,write:false}:operation==='delete'?{docId:doc}:{docId:doc,content,mediaType:'text/plain',expectedVersion:version,create:operation==='ingest'}
   const response=await knowledgeOperation(kb,operation,body)
   if(typeof response.version==='number') setVersion(response.version)
   setResult(JSON.stringify(response,null,2))
  } catch(e) {setResult(e instanceof Error?e.message:'Knowledge request failed')}
  finally {setBusy(false)}
 }
 return <section aria-label="Governed knowledge" style={{padding:24,display:'grid',gap:12,maxWidth:900}}>
  <p>Add plain text or Markdown to your knowledge collection. Access follows your signed-in account.</p>
  <label>Collection reference<input value={kb} onChange={e=>setKb(e.target.value)} /></label>
  <label>Document reference<input value={doc} onChange={e=>{setDoc(e.target.value);setVersion(0)}} /></label>
  <label>Current document version<input type="number" min="0" value={version} onChange={e=>setVersion(Number(e.target.value))}/></label>
  <label>Document text<textarea rows={8} value={content} onChange={e=>setContent(e.target.value)}/></label>
  <label>Text or Markdown file<input type="file" accept=".txt,.md,text/plain,text/markdown" onChange={async e=>{const file=e.target.files?.[0];if(!file)return;if(file.size>5242880){setResult('Files must be 5 MB or smaller.');return}setContent(await file.text())}}/></label>
  <div><button disabled={busy||!content.trim()} onClick={()=>void perform(version===0?'ingest':'update')}>Save document</button> <button disabled={busy||version===0} onClick={()=>void perform('delete')}>Delete document</button></div>
  <label>Search your knowledge<input value={query} onChange={e=>setQuery(e.target.value)}/></label>
  <button disabled={busy||!query.trim()} onClick={()=>void perform('query')}>Search</button>
  <label>Share with user reference<input value={member} onChange={e=>setMember(e.target.value)}/></label>
  <button disabled={busy||!member.trim()} onClick={()=>void perform('share')}>Share document for reading</button>
  <pre role="status" style={{whiteSpace:'pre-wrap'}}>{result}</pre>
 </section>
}
