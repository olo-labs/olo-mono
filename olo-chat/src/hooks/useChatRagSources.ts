/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { useCallback, useEffect, useState } from 'react'
import { knowledgeCollections } from '../api/governedKnowledgeApi'
export function useChatRagSources() {
 const [sources,setSources]=useState<string[]>([])
 const refresh=useCallback(async()=>{try {setSources(await knowledgeCollections())} catch {setSources([])}},[])
 useEffect(()=>{void refresh()},[refresh])
 return {sources,refresh}
}
